package com.stateworks.client.model;

import com.stateworks.client.StateworksVisualOverlay;
import com.stateworks.client.StateworksVisualStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

import java.util.List;
import java.util.Map;

/**
 * Selects a pre-baked resource-pack model during a Stateworks transition.
 * Stateworks does not transform or interpolate model geometry.
 *
 * <p>Transition frame sets may contain property-aware variants. The most
 * specific matching variant is selected from the current vanilla BlockState,
 * allowing blocks such as doors and trapdoors to retain their visual
 * properties while OPEN is animated.</p>
 */
public final class VirtualStateBlockModel implements DynamicBlockStateModel {
    private final BlockStateModel original;
    private final Identifier blockId;
    private final Map<String, List<StateworksVisualOverlay.VisualVariant>> frames;

    public VirtualStateBlockModel(
            BlockStateModel original,
            Identifier blockId,
            Map<String, List<StateworksVisualOverlay.VisualVariant>> frames
    ) {
        this.original = original;
        this.blockId = blockId;
        this.frames = Map.copyOf(frames);
        System.out.println("[Stateworks MODEL] Created wrapper for " + blockId + " transitions=" + this.frames.keySet());
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        System.out.println("[Stateworks MODEL] collectParts invoked block=" + blockId + " pos=" + pos + " state=" + state);

        var dimension = level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel
                ? clientLevel.dimension()
                : Minecraft.getInstance().level.dimension();
        var visual = StateworksVisualStateManager.get(dimension, pos);
        long now = System.currentTimeMillis();

        if (visual == null) {
            original.collectParts(level, pos, state, random, parts);
            return;
        }

        if (!visual.isTransitioning(now)) {
            original.collectParts(level, pos, state, random, parts);
            return;
        }

        String transitionName = visual.transitionStateName(pos);
        System.out.println(
                "[Stateworks TRACE] Render lookup: pos=" + pos
                        + " transition=" + transitionName
                        + " available=" + frames.keySet()
        );

        List<StateworksVisualOverlay.VisualVariant> variants = frames.get(transitionName);
        if (variants == null || variants.isEmpty()) {
            original.collectParts(level, pos, state, random, parts);
            return;
        }

        StateworksVisualOverlay.VisualVariant variant =
                selectVariant(variants, state);

        if (variant == null) {
            original.collectParts(level, pos, state, random, parts);
            return;
        }

        Direction facing = getFacing(state);
        List<BlockStateModel> transitionFrames = variant.directionalFrames().get(facing);
        if (transitionFrames == null || transitionFrames.isEmpty()) {
            original.collectParts(level, pos, state, random, parts);
            return;
        }

        float progress = visual.getProgress(now);
        int frameCount = transitionFrames.size();

        // All frame counts are valid. A 4-tick transition can therefore use
        // 2, 4, 8, 16, or any other number of frames without changing Java.
        int frame = Math.max(
                1,
                Math.min(
                        frameCount,
                        (int) Math.ceil(progress * frameCount)
                )
        );

        transitionFrames.get(frame - 1).collectParts(
                level,
                pos,
                state,
                random,
                parts
        );
    }

    /**
     * Chooses the most specific matching property variant. A default variant
     * has zero conditions and therefore acts as a fallback.
     */
    private static StateworksVisualOverlay.VisualVariant selectVariant(
            List<StateworksVisualOverlay.VisualVariant> variants,
            BlockState state
    ) {
        StateworksVisualOverlay.VisualVariant best = null;
        int bestSpecificity = -1;

        for (StateworksVisualOverlay.VisualVariant variant : variants) {
            if (!variant.matches(state)) {
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

    private static Direction getFacing(BlockState state) {
        if (state != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Material.Baked particleMaterial() {
        return original.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return original.materialFlags();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.materialFlags(level, pos, state);
    }
}
