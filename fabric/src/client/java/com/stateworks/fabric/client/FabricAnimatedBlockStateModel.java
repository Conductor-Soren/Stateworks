package com.stateworks.fabric.client;

import com.stateworks.client.StateworksVisualStateManager;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects an already-baked resource-pack frame while a Stateworks transition
 * is active. Geometry and texture baking remain owned by Minecraft/Fabric.
 */
final class FabricAnimatedBlockStateModel extends WrapperBlockStateModel {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stateworks Fabric Timeline");
    private static final Map<String, Long> LAST_TRACE_LOG = new ConcurrentHashMap<>();
    private final Map<String, List<FabricVisualModelPlugin.LoadedVariant>> transitions;

    FabricAnimatedBlockStateModel(
            BlockStateModel wrapped,
            Map<String, List<FabricVisualModelPlugin.LoadedVariant>> transitions
    ) {
        super(wrapped);
        this.transitions = Map.copyOf(transitions);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest
    ) {
        trace("emit/" + pos.asLong(), 1000L,
                "[Stateworks CLIENT EMIT] emitQuads reached pos={} state={} levelType={} transitionVariants={}",
                pos, state, level.getClass().getName(), transitions.size());
        ExtraModelKey<BlockStateModel> key = selectFrameKey(level, pos, state);
        if (key == null) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        BlockStateModel frame = getModel(key);
        if (frame == null) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        ((FabricBlockStateModel) frame).emitQuads(
                emitter,
                level,
                pos,
                state,
                random,
                cullTest
        );
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        trace("geometry/" + pos.asLong(), 1000L,
                "[Stateworks CLIENT GEOMETRY] createGeometryKey reached pos={} state={} levelType={}",
                pos, state, level.getClass().getName());
        ExtraModelKey<BlockStateModel> key = selectFrameKey(level, pos, state);
        if (key == null) {
            return super.createGeometryKey(level, pos, state, random);
        }
        return new VisualGeometryKey(key);
    }

    private ExtraModelKey<BlockStateModel> selectFrameKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        // Chunk rendering supplies a RenderSectionRegion (a render snapshot),
        // not the live ClientLevel. The active client world is the authoritative
        // source for the dimension key used by StateworksVisualStateManager.
        // Keep the render-region level for model emission, but read transition
        // state through Minecraft's current ClientLevel.
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            trace("select/no-client-level/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] skipped: Minecraft has no active ClientLevel; pos={} renderLevelType={}",
                    pos, level == null ? "null" : level.getClass().getName());
            return null;
        }

        StateworksVisualStateManager.VisualState visual =
                StateworksVisualStateManager.get(clientLevel.dimension(), pos);
        long now = System.currentTimeMillis();

        if (visual == null) {
            trace("select/no-visual/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] no visual state for pos={} dimension={}",
                    pos, clientLevel.dimension());
            return null;
        }
        if (!visual.isTransitioning(now)) {
            trace("select/inactive/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] visual state is not transitioning pos={} transition={} progress={}",
                    pos, visual.visualTransitionName(), visual.getProgress(now));
            return null;
        }

        List<FabricVisualModelPlugin.LoadedVariant> variants =
                transitions.get(visual.transitionStateName(pos));
        if (variants == null || variants.isEmpty()) {
            trace("select/no-variant/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] no visual variant for pos={} transitionState={} availableTransitions={}",
                    pos, visual.transitionStateName(pos), transitions.keySet());
            return null;
        }

        FabricVisualModelPlugin.LoadedVariant selected = selectVariant(variants, state);
        if (selected == null) {
            trace("select/no-match/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] no variant conditions match pos={} state={} transitionState={}",
                    pos, state, visual.transitionStateName(pos));
            return null;
        }

        FabricVisualModelPlugin.ModelOrientation orientation =
                FabricVisualModelPlugin.orientationFor(state);
        List<ExtraModelKey<BlockStateModel>> frames =
                selected.directionalKeys().get(orientation);
        if (frames == null || frames.isEmpty()) {
            trace("select/no-direction/" + pos.asLong(), 2000L,
                    "[Stateworks CLIENT SELECT] no frames for orientation pos={} orientation={} transitionState={} availableOrientations={}",
                    pos, orientation, visual.transitionStateName(pos), selected.directionalKeys().keySet());
            return null;
        }

        float progress = visual.getProgress(now);
        int frameIndex = selectFrameIndex(selected, progress, frames.size());

        // Throttled client-side trace: reports both timeline progress and the
        // frame selected by the dynamic model, without flooding latest.log.
        String traceKey = clientLevel.dimension().toString() + "/" + pos.asLong();
        Long lastTrace = LAST_TRACE_LOG.get(traceKey);
        if (lastTrace == null || now - lastTrace >= 250L) {
            LAST_TRACE_LOG.put(traceKey, now);
            long elapsed = Math.max(0L, now - visual.transitionStartTime());
            LOGGER.info(
                    "[Stateworks CLIENT FRAME] pos={} transition={} progress={} frame={}/{} elapsedMs={} durationMs={} orientation={}",
                    pos,
                    visual.visualTransitionName(),
                    String.format(java.util.Locale.ROOT, "%.3f", progress),
                    frameIndex + 1,
                    frames.size(),
                    elapsed,
                    visual.transitionDuration(),
                    orientation
            );
        }

        return frames.get(frameIndex);
    }

    private static FabricVisualModelPlugin.LoadedVariant selectVariant(
            List<FabricVisualModelPlugin.LoadedVariant> variants,
            BlockState state
    ) {
        FabricVisualModelPlugin.LoadedVariant best = null;
        int bestSpecificity = -1;

        for (FabricVisualModelPlugin.LoadedVariant variant : variants) {
            if (!FabricVisualModelPlugin.matches(variant.conditions(), state)) {
                continue;
            }

            int specificity = variant.conditions().size();
            if (specificity > bestSpecificity) {
                best = variant;
                bestSpecificity = specificity;
            }
        }
        return best;
    }

    private static int selectFrameIndex(
            FabricVisualModelPlugin.LoadedVariant variant,
            float progress,
            int frameCount
    ) {
        if (variant.durations().isEmpty()) {
            return Math.max(
                    0,
                    Math.min(
                            frameCount - 1,
                            (int) Math.ceil(progress * frameCount) - 1
                    )
            );
        }

        long totalDuration = Math.max(1L, variant.totalDuration());
        long elapsed = Math.min(
                totalDuration - 1L,
                Math.max(0L, (long) Math.floor(progress * totalDuration))
        );

        long accumulated = 0L;
        for (int index = 0; index < frameCount; index++) {
            long duration = index < variant.durations().size()
                    ? Math.max(1L, variant.durations().get(index))
                    : 1L;
            accumulated += duration;
            if (elapsed < accumulated) {
                return index;
            }
        }

        return frameCount - 1;
    }

    private static void trace(String key, long intervalMs, String message, Object... args) {
        long now = System.currentTimeMillis();
        Long previous = LAST_TRACE_LOG.putIfAbsent(key, now);
        if (previous == null || now - previous >= intervalMs) {
            LAST_TRACE_LOG.put(key, now);
            LOGGER.info(message, args);
        }
    }

    private static BlockStateModel getModel(ExtraModelKey<BlockStateModel> key) {
        Minecraft minecraft = Minecraft.getInstance();
        return ((FabricModelManager) minecraft.getModelManager()).getModel(key);
    }

    private record VisualGeometryKey(ExtraModelKey<BlockStateModel> frame) {
    }
}
