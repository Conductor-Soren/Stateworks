package com.stateworks.client;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StateworksVisualStateManager {

    private static final Map<Key, VisualState> STATES =
            new ConcurrentHashMap<>();

    private StateworksVisualStateManager() {
    }

    public static void set(
            ResourceKey<Level> dimension,
            BlockPos position,
            String stateSetId,
            String stateName
    ) {
        set(
                dimension,
                position,
                stateSetId,
                stateName,
                null,
                0L,
                0L,
                Map.of(),
                Map.of()
        );
    }

    public static void set(
            ResourceKey<Level> dimension,
            BlockPos position,
            String stateSetId,
            String stateName,
            String previousStateName,
            long transitionElapsed,
            long transitionDuration,
            Map<String, Double> previousSignals,
            Map<String, Double> signals
    ) {
        if (dimension == null) {
            throw new IllegalArgumentException(
                    "Stateworks visual dimension cannot be null"
            );
        }

        if (position == null) {
            throw new IllegalArgumentException(
                    "Stateworks visual position cannot be null"
            );
        }

        if (stateSetId == null
                || stateSetId.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks visual state set ID cannot be null or blank"
            );
        }

        if (stateName == null
                || stateName.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks visual state name cannot be null or blank"
            );
        }

        long safeDuration =
                Math.max(
                        0L,
                        transitionDuration
                );

        long safeElapsed =
                Math.max(
                        0L,
                        Math.min(
                                transitionElapsed,
                                safeDuration
                        )
                );

        long clientStartTime =
                System.currentTimeMillis()
                        - safeElapsed;

        STATES.put(
                new Key(
                        dimension,
                        position.immutable()
                ),
                new VisualState(
                        stateSetId,
                        stateName,
                        previousStateName,
                        clientStartTime,
                        safeDuration,
                        previousSignals == null ? Map.of() : Map.copyOf(previousSignals),
                        signals == null ? Map.of() : Map.copyOf(signals)
                )
        );
    }

    public static VisualState get(
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
        if (dimension == null
                || position == null) {
            return null;
        }

        return STATES.get(
                new Key(
                        dimension,
                        position.immutable()
                )
        );
    }

    public static String getStateName(
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
        VisualState state =
                get(
                        dimension,
                        position
                );

        return state != null
                ? state.stateName()
                : null;
    }

    public static String getStateName(
            BlockPos pos
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        return getStateName(
                minecraft.level.dimension(),
                pos
        );
    }

    /**
     * Returns the state that should currently be displayed.
     *
     * During a transition, the previous state is displayed.
     * Once the transition completes, the destination state
     * is displayed.
     */
    public static String getDisplayedStateName(
            ResourceKey<Level> dimension,
            BlockPos position,
            long currentTime
    ) {
        VisualState state =
                get(
                        dimension,
                        position
                );

        if (state == null) {
            return null;
        }

        if (!state.isTransitioning(
                currentTime
        )) {
            return state.stateName();
        }

        return state.previousStateName();
    }

    public static String getDisplayedStateName(
            BlockPos position,
            long currentTime
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        return getDisplayedStateName(
                minecraft.level.dimension(),
                position,
                currentTime
        );
    }

    public static float getProgress(
            ResourceKey<Level> dimension,
            BlockPos position,
            long currentTime
    ) {
        VisualState state =
                get(
                        dimension,
                        position
                );

        if (state == null) {
            return 1.0F;
        }

        return state.getProgress(
                currentTime
        );
    }

    public static float getProgress(
            BlockPos position,
            long currentTime
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return 1.0F;
        }

        return getProgress(
                minecraft.level.dimension(),
                position,
                currentTime
        );
    }

    public static double getSignal(
            ResourceKey<Level> dimension,
            BlockPos position,
            String signalName
    ) {
        VisualState state = get(dimension, position);
        if (state == null || signalName == null) {
            return 0.0D;
        }
        double previous = state.previousSignals()
                .getOrDefault(
                        signalName,
                        state.signals().getOrDefault(signalName, 0.0D)
                );
        double next = state.signals()
                .getOrDefault(signalName, previous);

        return SignalInterpolator.linear(
                previous,
                next,
                state.getProgress(System.currentTimeMillis())
        );
    }

    public static double getSignal(
            BlockPos position,
            String signalName
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0D;
        }
        return getSignal(minecraft.level.dimension(), position, signalName);
    }

    public static String getStateSetId(
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
        VisualState state =
                get(
                        dimension,
                        position
                );

        return state != null
                ? state.stateSetId()
                : null;
    }

    /**
     * Advances client-side visual transitions and invalidates the
     * affected block geometry so the dynamic model can rebuild.
     */
    public static void tick() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || minecraft.levelRenderer == null) {
            return;
        }

        ResourceKey<Level> dimension =
                minecraft.level.dimension();

        long currentTime =
                System.currentTimeMillis();

        for (Map.Entry<Key, VisualState> entry :
                STATES.entrySet()) {

            Key key = entry.getKey();
            VisualState state = entry.getValue();

            if (!key.dimension().equals(dimension)) {
                continue;
            }

            if (state.isTransitioning(currentTime)) {
                minecraft.levelRenderer.setBlocksDirty(
                        key.position().getX(),
                        key.position().getY(),
                        key.position().getZ(),
                        key.position().getX(),
                        key.position().getY(),
                        key.position().getZ()
                );
                continue;
            }

            /*
             * The four-frame visual transition has finished.  Dirty the block
             * one final time so the renderer drops the transition frame and
             * returns to Minecraft's normal blockstate model, then remove the
             * completed visual state from the client cache.
             */
            minecraft.levelRenderer.setBlocksDirty(
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ(),
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ()
            );

            STATES.remove(key, state);
        }
    }

    public static void remove(
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
        if (dimension == null
                || position == null) {
            return;
        }

        STATES.remove(
                new Key(
                        dimension,
                        position.immutable()
                )
        );
    }

    public static void clear() {
        STATES.clear();
    }

    public record VisualState(
            String stateSetId,
            String stateName,
            String previousStateName,
            long transitionStartTime,
            long transitionDuration,
            Map<String, Double> previousSignals,
            Map<String, Double> signals
    ) {

        public VisualState {
            previousSignals = previousSignals == null
                    ? Map.of()
                    : Map.copyOf(previousSignals);
            signals = signals == null
                    ? Map.of()
                    : Map.copyOf(signals);
        }

        public boolean isTransitioning(
                long currentTime
        ) {
            return previousStateName != null
                    && !previousStateName.equals(
                    stateName
            )
                    && transitionDuration > 0L
                    && currentTime
                    < transitionStartTime
                    + transitionDuration;
        }

        /**
         * Returns the visual transition state name used by resource-pack model
         * mappings. For example, closed -> open becomes "opening", while
         * open -> closed becomes "PoweringOff". Stateworks 1.0 only exposes
         * hard-coded virtual transition names.
         */
        public String transitionStateName(BlockPos position) {
            if (previousStateName == null
                    || previousStateName.equals(stateName)) {
                return null;
            }

            // Built-in visual transitions are deliberately fixed and hard-coded
            // for Stateworks 1.0. Resource packs consume these names; they do
            // not define the transitions themselves.
            if ("example:repeater_power".equals(stateSetId)) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level != null && position != null) {
                    BlockState blockState = minecraft.level.getBlockState(position);
                    if (blockState.getBlock() instanceof RepeaterBlock) {
                        int delay = blockState.getValue(RepeaterBlock.DELAY);
                        if ("off".equals(previousStateName) && "active".equals(stateName)) {
                            return "Powering_" + delay;
                        }
                        if ("active".equals(previousStateName) && "off".equals(stateName)) {
                            return "PoweringOff_" + delay;
                        }
                    }
                }
            }
            if ("cold".equals(previousStateName) && "hot".equals(stateName)
                    && "example:furnace_heat".equals(stateSetId)) {
                return "Heating";
            }
            if ("hot".equals(previousStateName) && "cold".equals(stateName)
                    && "example:furnace_heat".equals(stateSetId)) {
                return "Cooling";
            }

            return null;
        }

        public float getProgress(
                long currentTime
        ) {
            if (!isTransitioning(
                    currentTime
            )) {
                return 1.0F;
            }

            long elapsed =
                    currentTime
                            - transitionStartTime;

            if (elapsed <= 0L) {
                return 0.0F;
            }

            return Math.min(
                    1.0F,
                    elapsed
                            / (float) transitionDuration
            );
        }

        /**
         * Returns a signal interpolated between the values captured
         * at the start and end of the current transition.
         */
        public double getInterpolatedSignal(
                String signalName,
                long currentTime
        ) {
            if (signalName == null) {
                return 0.0D;
            }

            double current =
                    signals.getOrDefault(
                            signalName,
                            0.0D
                    );

            if (!isTransitioning(currentTime)) {
                return current;
            }

            double previous =
                    previousSignals.getOrDefault(
                            signalName,
                            current
                    );

            double progress =
                    getProgress(currentTime);

            return previous
                    + (current - previous) * progress;
        }
    }

    private record Key(
            ResourceKey<Level> dimension,
            BlockPos position
    ) {
    }
}