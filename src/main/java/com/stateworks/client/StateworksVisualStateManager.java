package com.stateworks.client;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.minecraft.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.concurrent.ConcurrentHashMap;

public final class StateworksVisualStateManager {

    private static volatile ToLongFunction<String> visualDurationResolver = ignored -> 0L;

    public static void setVisualDurationResolver(ToLongFunction<String> resolver) {
        visualDurationResolver = resolver == null ? ignored -> 0L : resolver;
    }

    private static final Map<Key, VisualState> STATES =
            new ConcurrentHashMap<>();

    private static final Map<Key, Long> LAST_TIMELINE_TRACE =
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
            String visualTransitionName,
            long transitionElapsed,
            long transitionDuration,
            Map<String, Double> previousSignals,
            Map<String, Double> signals
    ) {
        System.out.println(
                "[Stateworks TRACE] VisualStateManager.set: pos=" + position
                        + " set=" + stateSetId
                        + " state=" + stateName
                        + " previous=" + previousStateName
                        + " visual=" + visualTransitionName
                        + " elapsed=" + transitionElapsed
                        + " duration=" + transitionDuration
        );

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

        // Network/resource-pack transition times are expressed in Minecraft
        // ticks. VisualState stores its clock in milliseconds because the
        // client renderer uses System.currentTimeMillis(). Keep the units
        // explicit at this boundary so a 12-tick animation lasts 600 ms,
        // not 12 ms.
        Key key = new Key(dimension, position.immutable());
        VisualState existing = STATES.get(key);

        /*
         * The server may send several payloads while one logical transition
         * is in progress. Do not restart the client-side visual timeline on
         * every packet. The first packet establishes the visual start time;
         * subsequent packets only refresh the state/signal data.
         *
         * This is especially important when a visual transition is shorter
         * than the logical transition. For example, a 12-tick Jukebox
         * animation can finish while the Boolean transition is still 50
         * ticks long. Rebuilding the start time from the server's later
         * elapsed value would otherwise clamp the elapsed time to 12 ticks
         * and make the animation repeatedly appear/disappear or skip frames.
         */
        boolean sameTransition =
                existing != null
                        && java.util.Objects.equals(existing.stateName(), stateName)
                        && java.util.Objects.equals(existing.previousStateName(), previousStateName)
                        && java.util.Objects.equals(existing.visualTransitionName(), visualTransitionName);

        long durationMillis = Math.max(0L, transitionDuration);

        // TransitionDuration is already expressed in milliseconds. Only the
        // explicit resource-pack visual duration is converted from Minecraft
        // ticks (50 ms each) into milliseconds.
        long visualDurationTicks =
                visualDurationResolver.applyAsLong(visualTransitionName);

        if (visualDurationTicks > 0L) {
            durationMillis =
                    Math.multiplyExact(visualDurationTicks, 50L);
        }

        long safeDuration = durationMillis;

        long clientStartTime;

        if (sameTransition && existing.transitionDuration() == safeDuration) {
            // Preserve the original visual clock. This packet is only a
            // state/signal refresh, not a new visual transition.
            clientStartTime = existing.transitionStartTime();
        } else {
            // Network transitionElapsed and transitionDuration are already
            // milliseconds. Do not multiply them by 50.
            long safeElapsed =
                    Math.max(
                            0L,
                            Math.min(
                                    transitionElapsed,
                                    safeDuration
                            )
                    );

            clientStartTime =
                    System.currentTimeMillis()
                            - safeElapsed;
        }

        STATES.put(
                key,
                new VisualState(
                        stateSetId,
                        stateName,
                        previousStateName,
                        visualTransitionName,
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
                Long lastTrace = LAST_TIMELINE_TRACE.get(key);
                if (lastTrace == null || currentTime - lastTrace >= 250L) {
                    LAST_TIMELINE_TRACE.put(key, currentTime);
                    System.out.println(
                            "[Stateworks CLIENT TIMELINE] pos=" + key.position()
                                    + " visual=" + state.visualTransitionName()
                                    + " progress=" + state.getProgress(currentTime)
                                    + " elapsedMs=" + Math.max(0L, currentTime - state.transitionStartTime())
                                    + " durationMs=" + state.transitionDuration()
                    );
                }
                minecraft.levelExtractor.setBlocksDirty(
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
             * The visual transition has finished.  Dirty the block
             * one final time so the renderer drops the transition frame and
             * returns to Minecraft's normal blockstate model, then remove the
             * completed visual state from the client cache.
             */
            minecraft.levelExtractor.setBlocksDirty(
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ(),
                    key.position().getX(),
                    key.position().getY(),
                    key.position().getZ()
            );

            STATES.remove(key, state);
            LAST_TIMELINE_TRACE.remove(key);
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
        LAST_TIMELINE_TRACE.clear();
    }

    public record VisualState(
            String stateSetId,
            String stateName,
            String previousStateName,
            String visualTransitionName,
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
            return visualTransitionName;
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