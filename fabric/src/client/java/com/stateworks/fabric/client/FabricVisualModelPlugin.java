package com.stateworks.fabric.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fabric model-loading adapter for Stateworks resource-pack animations.
 *
 * <p>The visual JSON format and frame timing match the NeoForge adapter.
 * Minecraft still loads and bakes every referenced model, including its
 * normal parent/child inheritance and texture variables.</p>
 */
public final class FabricVisualModelPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stateworks Visuals");
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST
    };

    private static volatile Map<Identifier, Map<String, List<LoadedVariant>>> VISUALS =
            Map.of();
    private static volatile Map<String, Long> TRANSITION_DURATIONS = Map.of();

    private FabricVisualModelPlugin() {
    }

    public static void register() {
        PreparableModelLoadingPlugin.register(
                (sharedState, executor) -> CompletableFuture.supplyAsync(
                        () -> readDefinitions(sharedState.resourceManager()),
                        executor
                ),
                FabricVisualModelPlugin::initialize
        );
    }

    public static long getTransitionDuration(String transitionName) {
        if (transitionName == null) {
            return 0L;
        }
        return TRANSITION_DURATIONS.getOrDefault(transitionName, 0L);
    }

    private static List<BlockDefinition> readDefinitions(ResourceManager resources) {
        List<BlockDefinition> definitions = new ArrayList<>();
        Map<String, Long> durations = new HashMap<>();

        Map<Identifier, Resource> files = resources.listResources(
                "stateworks",
                id -> id.getPath().startsWith("stateworks/visuals/")
                        && id.getPath().endsWith(".json")
        );

        LOGGER.info("[Stateworks Fabric] Found {} visual definition resource(s)", files.size());

        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            Identifier resourceId = entry.getKey();

            try (Reader reader = new InputStreamReader(
                    entry.getValue().open(),
                    StandardCharsets.UTF_8
            )) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Identifier blockId = Identifier.parse(root.get("block").getAsString());
                JsonObject animations = root.getAsJsonObject("animations");

                if (animations == null) {
                    throw new IllegalArgumentException(
                            "Visual definition must contain an 'animations' object"
                    );
                }

                Map<String, List<VariantDefinition>> transitions = new HashMap<>();
                for (Map.Entry<String, JsonElement> animationEntry : animations.entrySet()) {
                    List<VariantDefinition> variants = readAnimation(
                            animationEntry.getValue(),
                            resources,
                            animationEntry.getKey()
                    );
                    transitions.put(animationEntry.getKey(), variants);

                    // Only explicitly authored JSON durations may override the
                    // Stateworks transition duration. readDurations() fills
                    // omitted values with 1 tick for frame selection, so checking
                    // durations().isEmpty() here incorrectly treats every
                    // animation as having custom timing. That caused repeater
                    // variants to all inherit the longest frame count (4 ticks),
                    // overriding their Java delay * 2 timing (6 or 8 ticks).
                    long duration = variants.stream()
                            .filter(VariantDefinition::hasExplicitDurations)
                            .mapToLong(VariantDefinition::totalDuration)
                            .max()
                            .orElse(0L);
                    if (duration > 0L) {
                        durations.merge(animationEntry.getKey(), duration, Math::max);
                    }
                }

                definitions.add(new BlockDefinition(blockId, Map.copyOf(transitions)));
                LOGGER.info(
                        "[Stateworks Fabric] Read visual definition {} for {}",
                        resourceId,
                        blockId
                );
            } catch (Exception exception) {
                LOGGER.warn(
                        "[Stateworks Fabric] Failed to read visual definition {}",
                        resourceId,
                        exception
                );
            }
        }

        TRANSITION_DURATIONS = Map.copyOf(durations);
        return List.copyOf(definitions);
    }

    private static List<VariantDefinition> readAnimation(
            JsonElement element,
            ResourceManager resources,
            String animationName
    ) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Animation '" + animationName + "' must be an object"
            );
        }

        JsonObject animation = element.getAsJsonObject();
        List<VariantDefinition> variants = new ArrayList<>();

        if (animation.has("frames")) {
            List<Identifier> frames = readFrames(
                    animation.get("frames"),
                    resources,
                    animationName
            );
            variants.add(new VariantDefinition(
                    Map.of(),
                    frames,
                    readDurations(animation.get("durations"), frames.size()),
                    animation.has("durations")
            ));
        }

        if (animation.has("variants")) {
            JsonElement variantsElement = animation.get("variants");
            if (!variantsElement.isJsonArray()) {
                throw new IllegalArgumentException(
                        "'variants' for animation '" + animationName + "' must be an array"
                );
            }

            int index = 0;
            for (JsonElement variantElement : variantsElement.getAsJsonArray()) {
                index++;
                if (!variantElement.isJsonObject()) {
                    throw new IllegalArgumentException(
                            "Variant " + index + " for '" + animationName + "' must be an object"
                    );
                }

                JsonObject variant = variantElement.getAsJsonObject();
                Map<String, String> conditions = new HashMap<>();
                if (variant.has("when")) {
                    JsonElement when = variant.get("when");
                    if (!when.isJsonObject()) {
                        throw new IllegalArgumentException("'when' must be an object");
                    }
                    for (Map.Entry<String, JsonElement> condition : when.getAsJsonObject().entrySet()) {
                        if (!condition.getValue().isJsonPrimitive()) {
                            throw new IllegalArgumentException(
                                    "Variant conditions must be primitive string values"
                            );
                        }
                        conditions.put(
                                condition.getKey(),
                                condition.getValue().getAsString().toLowerCase(java.util.Locale.ROOT)
                        );
                    }
                }

                if (!variant.has("frames")) {
                    throw new IllegalArgumentException(
                            "Variant " + index + " for '" + animationName + "' is missing frames"
                    );
                }

                List<Identifier> frames = readFrames(
                        variant.get("frames"),
                        resources,
                        animationName + " variant " + index
                );
                variants.add(new VariantDefinition(
                        Map.copyOf(conditions),
                        frames,
                        readDurations(variant.get("durations"), frames.size()),
                        variant.has("durations")
                ));
            }
        }

        if (variants.isEmpty()) {
            throw new IllegalArgumentException(
                    "Animation '" + animationName + "' must define frames or variants"
            );
        }
        return List.copyOf(variants);
    }

    private static List<Identifier> readFrames(
            JsonElement element,
            ResourceManager resources,
            String description
    ) {
        if (!element.isJsonArray() || element.getAsJsonArray().isEmpty()) {
            throw new IllegalArgumentException(
                    "'frames' for " + description + " must be a non-empty array"
            );
        }

        List<Identifier> frames = new ArrayList<>();
        for (JsonElement frame : element.getAsJsonArray()) {
            if (!frame.isJsonPrimitive()) {
                throw new IllegalArgumentException(
                        "Every frame for " + description + " must be a model identifier"
                );
            }

            Identifier modelId = Identifier.parse(frame.getAsString());
            Identifier modelResource = Identifier.fromNamespaceAndPath(
                    modelId.getNamespace(),
                    "models/" + modelId.getPath() + ".json"
            );
            if (resources.getResource(modelResource).isEmpty()) {
                throw new IllegalArgumentException(
                        "Missing model resource '" + modelResource + "' referenced by " + description
                );
            }
            frames.add(modelId);
        }
        return List.copyOf(frames);
    }

    private static List<Long> readDurations(JsonElement element, int frameCount) {
        List<Long> durations = new ArrayList<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (!value.isJsonPrimitive()) {
                    break;
                }
                try {
                    durations.add(Math.max(1L, value.getAsLong()));
                } catch (RuntimeException ignored) {
                    break;
                }
            }
        }

        while (durations.size() < frameCount) {
            durations.add(1L);
        }
        if (durations.size() > frameCount) {
            durations = new ArrayList<>(durations.subList(0, frameCount));
        }
        return List.copyOf(durations);
    }

    private static void initialize(
            List<BlockDefinition> definitions,
            ModelLoadingPlugin.Context context
    ) {
        Map<Identifier, Map<String, List<LoadedVariant>>> loaded = new HashMap<>();

        for (BlockDefinition definition : definitions) {
            Block block = BuiltInRegistries.BLOCK.getValue(definition.blockId());
            if (block == null) {
                LOGGER.warn(
                        "[Stateworks Fabric] Visual definition references unknown block {}",
                        definition.blockId()
                );
                continue;
            }

            List<ModelOrientation> orientations = orientationsFor(block);
            Map<String, List<LoadedVariant>> transitions = new HashMap<>();

            for (Map.Entry<String, List<VariantDefinition>> transition :
                    definition.transitions().entrySet()) {
                List<LoadedVariant> loadedVariants = new ArrayList<>();

                for (VariantDefinition variant : transition.getValue()) {
                    Map<ModelOrientation, List<ExtraModelKey<BlockStateModel>>> directional =
                            new HashMap<>();

                    for (ModelOrientation orientation : orientations) {
                        List<ExtraModelKey<BlockStateModel>> frameKeys = new ArrayList<>();

                        for (Identifier frameId : variant.frameIds()) {
                            ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(
                                    () -> "stateworks:" + definition.blockId()
                                            + "/" + transition.getKey()
                                            + "/" + frameId
                                            + "/" + orientation
                            );
                            context.addModel(
                                    key,
                                    SimpleUnbakedExtraModel.blockStateModel(
                                            frameId,
                                            modelStateFor(orientation, definition.blockId(), block)
                                    )
                            );
                            frameKeys.add(key);
                        }

                        directional.put(orientation, List.copyOf(frameKeys));
                    }

                    loadedVariants.add(new LoadedVariant(
                            variant.conditions(),
                            variant.durations(),
                            Map.copyOf(directional)
                    ));
                }

                transitions.put(transition.getKey(), List.copyOf(loadedVariants));
            }

            loaded.put(definition.blockId(), Map.copyOf(transitions));
        }

        VISUALS = Map.copyOf(loaded);

        context.modifyBlockModelOnLoad().register((model, modifierContext) -> {
            BlockState state = modifierContext.state();
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            Map<String, List<LoadedVariant>> transitions = VISUALS.get(blockId);
            if (transitions == null || transitions.isEmpty()) {
                return model;
            }
            return new FabricVisualUnbakedRoot(model, transitions);
        });

        LOGGER.info(
                "[Stateworks Fabric] Registered visual models for {} block(s)",
                loaded.size()
        );
    }

    private static List<ModelOrientation> orientationsFor(Block block) {
        BlockState state = block.defaultBlockState();
        List<ModelOrientation> orientations = new ArrayList<>();

        if (block instanceof ButtonBlock) {
            for (AttachFace face : AttachFace.values()) {
                for (Direction direction : HORIZONTAL_DIRECTIONS) {
                    orientations.add(new ModelOrientation(face, direction));
                }
            }
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            for (Direction direction : Direction.values()) {
                orientations.add(new ModelOrientation(null, direction));
            }
        } else {
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                orientations.add(new ModelOrientation(null, direction));
            }
        }

        return List.copyOf(orientations);
    }

    private static OctahedralGroup rotationFor(
            ModelOrientation orientation,
            Identifier blockId,
            Block block
    ) {
        if (block instanceof ButtonBlock) {
            int y = switch (orientation.face()) {
                case FLOOR, WALL -> horizontalButtonRotation(orientation.facing());
                case CEILING -> switch (orientation.facing()) {
                    case NORTH -> 180;
                    case EAST -> 270;
                    case SOUTH -> 0;
                    case WEST -> 90;
                    default -> 0;
                };
            };
            int x = switch (orientation.face()) {
                case FLOOR -> 0;
                case WALL -> 90;
                case CEILING -> 180;
            };
            return Quadrant.fromXYAngles(quadrant(x), quadrant(y));
        }

        boolean northBase =
                blockId.equals(Identifier.parse("minecraft:furnace"))
                        || blockId.equals(Identifier.parse("minecraft:blast_furnace"))
                        || blockId.equals(Identifier.parse("minecraft:smoker"));

        return northBase
                ? switch (orientation.facing()) {
                    case NORTH -> OctahedralGroup.IDENTITY;
                    case EAST -> OctahedralGroup.BLOCK_ROT_Y_90;
                    case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_180;
                    case WEST -> OctahedralGroup.BLOCK_ROT_Y_270;
                    case UP -> OctahedralGroup.BLOCK_ROT_X_90;
                    case DOWN -> OctahedralGroup.BLOCK_ROT_X_270;
                }
                : switch (orientation.facing()) {
                    case SOUTH -> OctahedralGroup.IDENTITY;
                    case WEST -> OctahedralGroup.BLOCK_ROT_Y_90;
                    case NORTH -> OctahedralGroup.BLOCK_ROT_Y_180;
                    case EAST -> OctahedralGroup.BLOCK_ROT_Y_270;
                    case UP -> OctahedralGroup.BLOCK_ROT_X_90;
                    case DOWN -> OctahedralGroup.BLOCK_ROT_X_270;
                };
    }

    private static ModelState modelStateFor(
            ModelOrientation orientation,
            Identifier blockId,
            Block block
    ) {
        BlockModelRotation rotation =
                BlockModelRotation.get(rotationFor(orientation, blockId, block));
        return block instanceof ButtonBlock && orientation.face() == AttachFace.WALL
                ? rotation.withUvLock()
                : rotation;
    }

    private static int horizontalButtonRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private static Quadrant quadrant(int degrees) {
        return switch (degrees) {
            case 0 -> Quadrant.R0;
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> throw new IllegalArgumentException(
                    "Rotation must be a multiple of 90 degrees: " + degrees
            );
        };
    }

    static ModelOrientation orientationFor(BlockState state) {
        Direction facing = Direction.NORTH;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        }

        AttachFace face = state.hasProperty(BlockStateProperties.ATTACH_FACE)
                ? state.getValue(BlockStateProperties.ATTACH_FACE)
                : null;
        return new ModelOrientation(face, facing);
    }

    static boolean matches(Map<String, String> conditions, BlockState state) {
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            Property<?> property = null;
            for (Property<?> candidate : state.getProperties()) {
                if (candidate.getName().equals(entry.getKey())) {
                    property = candidate;
                    break;
                }
            }
            if (property == null
                    || !propertyName(property, state).equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static String propertyName(Property<?> property, BlockState state) {
        return propertyNameTyped(property, state);
    }

    private static <T extends Comparable<T>> String propertyNameTyped(
            Property<T> property,
            BlockState state
    ) {
        return property.getName(state.getValue(property));
    }

    record ModelOrientation(AttachFace face, Direction facing) {
    }

    record BlockDefinition(
            Identifier blockId,
            Map<String, List<VariantDefinition>> transitions
    ) {
    }

    record VariantDefinition(
            Map<String, String> conditions,
            List<Identifier> frameIds,
            List<Long> durations,
            boolean hasExplicitDurations
    ) {
        long totalDuration() {
            return durations.stream().mapToLong(value -> Math.max(1L, value)).sum();
        }
    }

    record LoadedVariant(
            Map<String, String> conditions,
            List<Long> durations,
            Map<ModelOrientation, List<ExtraModelKey<BlockStateModel>>> directionalKeys
    ) {
        long totalDuration() {
            return durations.stream().mapToLong(value -> Math.max(1L, value)).sum();
        }
    }

    static Map<String, List<LoadedVariant>> visualsFor(Identifier blockId) {
        return VISUALS.get(blockId);
    }
}
