package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public final class StateMachineManager {

    private final Map<MachineKey, MachineEntry> machines =
            new HashMap<>();

    public StateMachine getOrCreate(
            Level level,
            BlockPos pos,
            String stateSetId
    ) {
        MachineKey key =
                new MachineKey(
                        level.dimension(),
                        pos.immutable()
                );

        MachineEntry entry =
                machines.get(key);

        if (entry == null
                || !entry.stateSetId().equals(stateSetId)) {

            StateMachine machine =
                    new StateMachine(stateSetId);

            entry =
                    new MachineEntry(
                            level,
                            pos.immutable(),
                            stateSetId,
                            machine
                    );

            machines.put(
                    key,
                    entry
            );
        }

        return entry.machine();
    }

    public StateMachine update(
            Level level,
            BlockPos pos,
            String stateSetId,
            long currentTime
    ) {
        MachineKey key =
                new MachineKey(
                        level.dimension(),
                        pos.immutable()
                );

        StateContext context =
                new StateContext(
                        level,
                        pos
                );

        if (!StateRegistry.INSTANCE.applies(
                stateSetId,
                context
        )) {
            machines.remove(key);
            return null;
        }

        StateMachine machine =
                getOrCreate(
                        level,
                        pos,
                        stateSetId
                );

        machine.update(
                context,
                currentTime
        );

        machine.output()
                .apply(currentTime);

        MachineEntry entry =
                machines.get(key);

        if (entry != null) {
            updateVisualState(
                    entry,
                    currentTime
            );
        }

        return machine;
    }

    public void tick(
            long currentTime
    ) {
        var iterator =
                machines.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<MachineKey, MachineEntry> mapEntry =
                    iterator.next();

            MachineEntry entry =
                    mapEntry.getValue();

            /*
             * Integrated-server world reloads can leave this manager alive
             * briefly while the old ServerLevel/chunks are being torn down.
             * Never evaluate a retained machine against an unloaded position.
             */
            if (entry.level() instanceof ServerLevel serverLevel
                    && !serverLevel.isLoaded(entry.pos())) {
                iterator.remove();
                continue;
            }

            StateContext context =
                    new StateContext(
                            entry.level(),
                            entry.pos()
                    );

            if (!StateRegistry.INSTANCE.applies(
                    entry.stateSetId(),
                    context
            )) {
                iterator.remove();
                continue;
            }

            entry.machine().update(
                    context,
                    currentTime
            );

            entry.machine()
                    .output()
                    .apply(currentTime);

            updateVisualState(
                    entry,
                    currentTime
            );
        }
    }

    public void remove(
            Level level,
            BlockPos pos
    ) {
        machines.remove(
                new MachineKey(
                        level.dimension(),
                        pos.immutable()
                )
        );
    }

    public void clear() {
        machines.clear();
    }

    public int size() {
        return machines.size();
    }

    private record MachineKey(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
    }

    private static final class MachineEntry {

        private final Level level;
        private final BlockPos pos;
        private final String stateSetId;
        private final StateMachine machine;

        private String lastVisualState;
        private Map<String, Double> lastSignals = Map.of();

        private MachineEntry(
                Level level,
                BlockPos pos,
                String stateSetId,
                StateMachine machine
        ) {
            this.level = level;
            this.pos = pos;
            this.stateSetId = stateSetId;
            this.machine = machine;
        }

        public Level level() {
            return level;
        }

        public BlockPos pos() {
            return pos;
        }

        public String stateSetId() {
            return stateSetId;
        }

        public StateMachine machine() {
            return machine;
        }

        public String lastVisualState() {
            return lastVisualState;
        }

        public void setLastVisualState(
                String lastVisualState
        ) {
            this.lastVisualState = lastVisualState;
        }

        public Map<String, Double> lastSignals() {
            return lastSignals;
        }

        public void setLastSignals(
                Map<String, Double> lastSignals
        ) {
            this.lastSignals = lastSignals;
        }
    }

    private void updateVisualState(
            MachineEntry entry,
            long currentTime
    ) {
        if (!(entry.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        StateMachine machine =
                entry.machine();

        /*
         * During a transition, currentState() is already the
         * destination state while transition() retains the
         * previous state and timing.
         *
         * That is exactly the information we want to send
         * to the client.
         */
        VirtualState<?> currentState =
                machine.currentState();

        if (currentState == null) {
            return;
        }

        String stateName =
                currentState.name();

        Map<String, Double> signals =
                machine.signalValues();

        boolean stateChanged =
                !stateName.equals(entry.lastVisualState());

        boolean signalsChanged =
                !signals.equals(entry.lastSignals());

        /*
         * Signals are synchronized when the authoritative state changes,
         * or when there is no active transition. During a transition the
         * client owns the continuous interpolation, so ordinary server
         * ticks must not keep replacing its interpolation endpoints.
         */
        boolean transitioning = machine.isTransitioning(currentTime);

        if (!stateChanged && (!signalsChanged || transitioning)) {
            return;
        }

        Map<String, Double> previousSignals =
                entry.lastSignals();

        StateTransition transition =
                machine.transition();

        String previousStateName = null;
        long transitionElapsed = 0L;
        long transitionDuration = 0L;

        if (transition != null) {

            previousStateName =
                    transition.previous() != null
                            ? transition.previous().name()
                            : null;

            transitionDuration =
                    transition.duration();

            transitionElapsed =
                    Math.max(
                            0L,
                            currentTime
                                    - transition.startTime()
                    );

            transitionElapsed =
                    Math.min(
                            transitionElapsed,
                            transitionDuration
                    );
        }

        entry.setLastVisualState(
                stateName
        );
        entry.setLastSignals(signals);

        VisualStatePayload payload =
                new VisualStatePayload(
                        entry.pos(),
                        entry.stateSetId(),
                        stateName,
                        previousStateName,
                        transitionElapsed,
                        transitionDuration,
                        previousSignals,
                        signals
                );

        for (ServerPlayer player :
                serverLevel.players()) {

            PacketDistributor.sendToPlayer(
                    player,
                    payload
            );
        }
    }
}