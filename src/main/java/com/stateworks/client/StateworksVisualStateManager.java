package com.stateworks.client;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.block.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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

            if (!state.isTransitioning(currentTime)) {
                continue;
            }

            minecraft.levelRenderer.setBlocksDirty(
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ(),
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ()
            );
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