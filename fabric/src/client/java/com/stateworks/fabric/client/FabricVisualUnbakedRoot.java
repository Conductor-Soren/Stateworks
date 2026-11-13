package com.stateworks.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedRootBlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** Wraps a vanilla block-state root while leaving its normal bake path intact. */
final class FabricVisualUnbakedRoot extends WrapperUnbakedRootBlockStateModel {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stateworks Fabric Model Bake");
    private final Map<String, List<FabricVisualModelPlugin.LoadedVariant>> transitions;

    FabricVisualUnbakedRoot(
            BlockStateModel.UnbakedRoot wrapped,
            Map<String, List<FabricVisualModelPlugin.LoadedVariant>> transitions
    ) {
        super(wrapped);
        this.transitions = Map.copyOf(transitions);
    }

    @Override
    public BlockStateModel bake(BlockState state, ModelBaker baker) {
        LOGGER.info(
                "[Stateworks CLIENT BAKE] baking animated root block={} state={} transitionNames={} variantCount={}",
                state.getBlock(), state, transitions.keySet(),
                transitions.values().stream().mapToInt(List::size).sum()
        );
        BlockStateModel baked = wrapped.bake(state, baker);
        FabricAnimatedBlockStateModel animated = new FabricAnimatedBlockStateModel(
                baked,
                transitions
        );
        LOGGER.info(
                "[Stateworks CLIENT BAKE] animated wrapper created block={} state={} wrappedModelType={}",
                state.getBlock(), state, baked.getClass().getName()
        );
        return animated;
    }
}
