package com.stateworks.client.model;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.stateworks.Stateworks;
import com.stateworks.client.StateworksVisualStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VirtualStateBlockModel
        implements DynamicBlockStateModel {

    private static final int TRANSITION_STEPS = 20;

    private final BlockStateModelPart fallbackModel;
    private final BlockStateModelPart fallbackActiveModel;
    private final List<BlockStateModelPart> fallbackTransitionSteps;
    private final Map<String, BlockStateModelPart> stateModels;

    public VirtualStateBlockModel(
            BlockStateModelPart fallbackModel,
            BlockStateModelPart fallbackActiveModel,
            List<BlockStateModelPart> fallbackTransitionSteps,
            Map<String, BlockStateModelPart> stateModels
    ) {
        this.fallbackModel = fallbackModel;
        this.fallbackActiveModel = fallbackActiveModel;
        this.fallbackTransitionSteps = fallbackTransitionSteps;
        this.stateModels = Map.copyOf(stateModels);
    }

    private BlockStateModelPart modelFor(String stateName) {
        if (stateName == null) {
            return fallbackModel;
        }
        return stateModels.getOrDefault(stateName, fallbackModel);
    }

    @Override
    public Material.Baked particleMaterial() {
        return fallbackModel.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return fallbackModel.materialFlags();
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random
    ) {
        StateworksVisualStateManager.VisualState visual =
                getVisualState(
                        level,
                        pos
                );

        if (visual == null) {
            return new GeometryKey(
                    "off",
                    null,
                    TRANSITION_STEPS
            );
        }

        float activity =
                (float) visual.getInterpolatedSignal(
                        "activity",
                        System.currentTimeMillis()
                );

        int progressStep =
                Math.max(
                        0,
                        Math.min(
                                TRANSITION_STEPS,
                                Math.round(
                                        activity
                                                * TRANSITION_STEPS
                                )
                        )
                );

        return new GeometryKey(
                visual.stateName(),
                visual.previousStateName(),
                progressStep
        );
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        long currentTime = System.currentTimeMillis();

        StateworksVisualStateManager.VisualState visual =
                getVisualState(level, pos);

        if (visual == null) {
            parts.add(fallbackModel);
            return;
        }

        float progress = Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        visual.getProgress(currentTime)
                )
        );

        if (!visual.isTransitioning(currentTime)) {
            parts.add(modelFor(visual.stateName()));
            return;
        }

        // During a transition, reveal the destination state's visual
        // progressively. This replaces the old hard-coded active panel
        // with whatever model the Stateworks definition declares.
        BlockStateModelPart destination =
                modelFor(visual.stateName());

        if (progress >= 1.0F) {
            parts.add(destination);
            return;
        }

        int step = Math.max(
                0,
                Math.min(
                        TRANSITION_STEPS,
                        Math.round(progress * TRANSITION_STEPS)
                )
        );

        if (step > 0 && step < fallbackTransitionSteps.size()) {
            // Keep the legacy transition model as a compatibility fallback
            // when a dedicated transition asset is supplied.
            BlockStateModelPart transitionPart =
                    fallbackTransitionSteps.get(step);

            if (transitionPart != null) {
                parts.add(transitionPart);
            }
        } else if (step >= TRANSITION_STEPS) {
            parts.add(destination);
        }
    }

    @Override
    public Material.Baked particleMaterial(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        return fallbackModel.particleMaterial();
    }

    @Override
    public int materialFlags(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state
    ) {
        return fallbackModel.materialFlags();
    }

    public record Unbaked(
            Identifier offModel,
            Identifier activeModel,
            Identifier activeTransitionModel,
            String stateSet,
            int yRotation
    ) implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> CODEC =
                RecordCodecBuilder.mapCodec(
                        instance -> instance.group(
                                Identifier.CODEC
                                        .fieldOf("off")
                                        .forGetter(Unbaked::offModel),

                                Identifier.CODEC
                                        .fieldOf("active")
                                        .forGetter(Unbaked::activeModel),

                                Identifier.CODEC
                                        .fieldOf("active_transition")
                                        .forGetter(Unbaked::activeTransitionModel),

                                com.mojang.serialization.Codec.STRING
                                        .optionalFieldOf("state_set", "")
                                        .forGetter(Unbaked::stateSet),

                                com.mojang.serialization.Codec.INT
                                        .optionalFieldOf("y_rotation", 0)
                                        .forGetter(Unbaked::yRotation)

                        ).apply(instance, Unbaked::new)
                );

        public static final Identifier ID =
                Identifier.fromNamespaceAndPath(
                        Stateworks.MODID,
                        "virtual_state"
                );

        @Override
        public MapCodec<Unbaked> codec() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(
                ResolvableModel.Resolver resolver
        ) {
            resolver.markDependency(offModel);
            resolver.markDependency(activeModel);
            resolver.markDependency(activeTransitionModel);

            if (stateSet == null || stateSet.isBlank()) {
                return;
            }

            StateDefinitionSet<?> set =
                    StateRegistry.INSTANCE.getSet(stateSet);

            if (set == null) {
                Stateworks.LOGGER.warn(
                        "Virtual state model references unknown state set '{}'",
                        stateSet
                );
                return;
            }

            for (StateDefinition<?> definition : set.states()) {
                StateVisual visual = definition.visual();
                if (visual == null) {
                    continue;
                }

                resolver.markDependency(
                        Identifier.parse(visual.model())
                );
            }
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            BlockStateModelPart fallbackModel =
                    SimpleModelWrapper.bake(
                            baker,
                            offModel,
                            new RotationModelState(yRotation)
                    );

            BlockStateModelPart fallbackActiveModel =
                    SimpleModelWrapper.bake(
                            baker,
                            activeModel,
                            new RotationModelState(yRotation)
                    );

            List<BlockStateModelPart> fallbackTransitionSteps =
                    new ArrayList<>(TRANSITION_STEPS + 1);

            for (int step = 0;
                 step <= TRANSITION_STEPS;
                 step++) {

                float scale =
                        step / (float) TRANSITION_STEPS;

                fallbackTransitionSteps.add(
                        SimpleModelWrapper.bake(
                                baker,
                                activeTransitionModel,
                                new ScaleAndRotationModelState(
                                        scale,
                                        yRotation
                                )
                        )
                );
            }

            Map<String, BlockStateModelPart> stateModels =
                    new LinkedHashMap<>();

            if (stateSet != null && !stateSet.isBlank()) {
                StateDefinitionSet<?> set =
                        StateRegistry.INSTANCE.getSet(stateSet);

                if (set != null) {
                    for (StateDefinition<?> definition : set.states()) {
                        StateVisual visual = definition.visual();
                        if (visual == null) {
                            continue;
                        }

                        Identifier modelId =
                                Identifier.parse(visual.model());

                        BlockStateModelPart model =
                                SimpleModelWrapper.bake(
                                        baker,
                                        modelId,
                                        new RotationModelState(yRotation)
                                );

                        stateModels.put(
                                definition.name(),
                                model
                        );
                    }
                }
            }

            // The fallback models remain available so existing virtual_state
            // definitions continue to work even without a state_set field.
            return new VirtualStateBlockModel(
                    fallbackModel,
                    fallbackActiveModel,
                    List.copyOf(fallbackTransitionSteps),
                    stateModels
            );
        }
    }

    private static StateworksVisualStateManager.VisualState getVisualState(
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        if (level instanceof ClientLevel clientLevel) {
            return StateworksVisualStateManager.get(
                    clientLevel.dimension(),
                    pos
            );
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return null;
        }

        return StateworksVisualStateManager.get(
                minecraft.level.dimension(),
                pos
        );
    }

    private record GeometryKey(
            String stateName,
            String previousStateName,
            int progressStep
    ) {
    }

    private static final class ScaleAndRotationModelState
            implements ModelState {

        private final Transformation transformation;

        private ScaleAndRotationModelState(
                float scale,
                int yRotation
        ) {
            float radians = (float) Math.toRadians(yRotation);
            Matrix4f matrix =
                    new Matrix4f()
                            .translate(0.5F, 0.5F, 0.0F)
                            .rotateY(radians)
                            .scale(scale, scale, 1.0F)
                            .translate(-0.5F, -0.5F, 0.0F)
                            .translate(0.0F, 0.0F, -0.002F);

            this.transformation = new Transformation(matrix);
        }

        @Override
        public Transformation transformation() {
            return transformation;
        }

        @Override
        public Matrix4fc faceTransformation(Direction direction) {
            return transformation.getMatrix();
        }

        @Override
        public Matrix4fc inverseFaceTransformation(Direction direction) {
            return transformation.getMatrix();
        }
    }

    private static final class ScaleModelState
            implements ModelState {

        private final Transformation transformation;

        private ScaleModelState(float scale) {
            Matrix4f matrix =
                    new Matrix4f()
                            .translate(0.5F, 0.5F, 0.0F)
                            .scale(scale, scale, 1.0F)
                            .translate(-0.5F, -0.5F, 0.0F)
                            // Keep the transition panel on the front surface
                            // instead of scaling it back inside the solid fallbackModel.
                            .translate(0.0F, 0.0F, -0.002F);

            this.transformation =
                    new Transformation(matrix);
        }

        @Override
        public Transformation transformation() {
            return transformation;
        }

        @Override
        public Matrix4fc faceTransformation(
                Direction direction
        ) {
            return transformation.getMatrix();
        }

        @Override
        public Matrix4fc inverseFaceTransformation(
                Direction direction
        ) {
            return transformation.getMatrix();
        }
    }

    private static final class RotationModelState
            implements ModelState {

        private final Transformation transformation;

        private RotationModelState(int yRotation) {
            float radians = (float) Math.toRadians(yRotation);
            Matrix4f matrix =
                    new Matrix4f()
                            .translate(0.5F, 0.0F, 0.5F)
                            .rotateY(radians)
                            .translate(-0.5F, 0.0F, -0.5F);

            this.transformation = new Transformation(matrix);
        }

        @Override
        public Transformation transformation() {
            return transformation;
        }

        @Override
        public Matrix4fc faceTransformation(Direction direction) {
            return transformation.getMatrix();
        }

        @Override
        public Matrix4fc inverseFaceTransformation(Direction direction) {
            return transformation.getMatrix();
        }
    }

    private static final class IdentityModelState
            implements ModelState {

        private static final IdentityModelState INSTANCE =
                new IdentityModelState();

        private static final Transformation IDENTITY =
                new Transformation(
                        new Matrix4f()
                );

        @Override
        public Transformation transformation() {
            return IDENTITY;
        }

        @Override
        public Matrix4fc faceTransformation(
                Direction direction
        ) {
            return IDENTITY.getMatrix();
        }

        @Override
        public Matrix4fc inverseFaceTransformation(
                Direction direction
        ) {
            return IDENTITY.getMatrix();
        }
    }
}
