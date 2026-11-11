package com.stateworks.neoforge.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stateworks.neoforge.client.model.VirtualStateBlockModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.Direction;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional resource-pack visual overlay for Stateworks virtual transitions.
 *
 * <p>Normal Minecraft blockstate JSON files are never replaced or modified.
 * A visual definition only supplies pre-baked models that are selected while
 * a Stateworks transition is active.</p>
 *
 * <p>Each transition can have a default frame list or any number of property
 * variants. Variants are matched against the block's current vanilla
 * BlockState, allowing properties such as door HINGE/HALF or trapdoor HALF to
 * select the correct animation without changing the Stateworks registration.</p>
 */
public final class StateworksVisualOverlay {
    /*
     * Resource-pack visual transitions are the source of truth. Keys are the
     * configured transition names such as Heating, Cooling, or Powering.
     */
    private static final Map<String, List<VisualVariant>> RESOURCE_TRANSITIONS =
            new java.util.HashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Stateworks Visuals");
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTH,
            Direction.EAST
    };

    private static final Map<Identifier, Map<String, List<VisualVariant>>> MAPPINGS = new HashMap<>();
    private static final Map<Identifier, Map<String, List<List<StandaloneModelKey<BlockStateModel>>>>> KEYS = new HashMap<>();

    private StateworksVisualOverlay() {
    }

    public static long getTransitionDuration(String transitionName) {
        if (transitionName == null) return 0L;
        List<VisualVariant> variants = RESOURCE_TRANSITIONS.get(transitionName);
        if (variants == null || variants.isEmpty()) return 0L;

        // Only an explicit JSON "durations" array owns visual timing.
        // Animations without explicit frame timing use their logical
        // transition duration from the Stateworks definition.
        long max = 0L;
        boolean hasExplicitTiming = false;
        for (VisualVariant variant : variants) {
            if (variant.hasExplicitDurations()) {
                hasExplicitTiming = true;
                max = Math.max(max, variant.totalDuration());
            }
        }
        return hasExplicitTiming ? max : 0L;
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event, ResourceManager resources) {
        RESOURCE_TRANSITIONS.clear();
        MAPPINGS.clear();
        KEYS.clear();

        int definitionsFound = 0;

        // Search every resource-pack namespace. Visual definitions are intentionally
        // allowed to live in the pack's own namespace (for example
        // stateworks_example:stateworks/visuals/furnace.json), rather than
        // requiring the special "stateworks" namespace.
        Map<Identifier, net.minecraft.server.packs.resources.Resource> visualResources =
                resources.listResources(
                        "stateworks",
                        id -> id.getPath().startsWith("stateworks/visuals/")
                                && id.getPath().endsWith(".json")
                );

        LOGGER.info(
                "[Stateworks MODEL] Scanning stateworks resources; found {} candidate visual definition resource(s)",
                visualResources.size()
        );

        for (var entry : visualResources.entrySet()) {
            LOGGER.info(
                    "[Stateworks MODEL] Found visual definition resource {}",
                    entry.getKey()
            );
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Identifier block = Identifier.parse(root.get("block").getAsString());

                // The explicit animation format is the only supported format.
                // Each animation lists the exact Minecraft model resources to use.
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null) {
                    throw new IllegalArgumentException(
                            "Visual definition must contain an 'animations' object: "
                                    + entry.getKey()
                    );
                }
                Map<String, List<VisualVariant>> stateVariants = new HashMap<>();
                Map<String, List<List<StandaloneModelKey<BlockStateModel>>>> stateKeys = new HashMap<>();

                for (var stateEntry : animations.entrySet()) {
                    List<VisualVariant> variants = readExplicitAnimation(
                            stateEntry.getValue(), resources, stateEntry.getKey()
                    );
                    if (variants.isEmpty()) {
                        LOGGER.warn(
                                "[Stateworks] Animation '{}' in {} did not resolve any model frames",
                                stateEntry.getKey(), entry.getKey()
                        );
                        continue;
                    }

                    stateVariants.put(stateEntry.getKey(), List.copyOf(variants));

                    // Explicit state definitions (for example
                    // "inserting": {"model": "...", "durations": [...]})
                    // are also transition timing definitions. Keep their
                    // per-frame durations available to the visual state
                    // manager so the logical transition is not truncated to
                    // the first frame / one tick.
                    RESOURCE_TRANSITIONS.put(stateEntry.getKey(), List.copyOf(variants));

                    List<List<StandaloneModelKey<BlockStateModel>>> variantKeys = new ArrayList<>();
                    for (VisualVariant variant : variants) {
                        variantKeys.add(registerFrameModels(event, variant.frameIds(), block));
                    }
                    stateKeys.put(stateEntry.getKey(), List.copyOf(variantKeys));
                }

                if (!stateVariants.isEmpty()) {
                    MAPPINGS.put(block, Map.copyOf(stateVariants));
                    KEYS.put(block, Map.copyOf(stateKeys));
                    definitionsFound++;
                    LOGGER.info(
                            "[Stateworks MODEL] Loaded visual definition {} for {} with transitions {}",
                            entry.getKey(),
                            block,
                            stateVariants.keySet()
                    );
                }
            } catch (Exception exception) {
                LOGGER.warn(
                        "[Stateworks] Failed to load visual definition {}",
                        entry.getKey(),
                        exception
                );
            }
        }

        LOGGER.info(
                "[Stateworks MODEL] Resource visual discovery found {} definition(s)",
                definitionsFound
        );
    }

    /**
     * Reads optional per-frame timing from the explicit visual definition.
     * Missing entries default to one tick; extra entries are ignored.
     */
    private static List<Long> parseDurations(JsonElement element, int frameCount) {
        if (frameCount <= 0) {
            return List.of();
        }

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

    /**
     * Reads the explicit animation format. Every frame is a normal Minecraft
     * model identifier (namespace:block/path), so model parent inheritance,
     * texture variables, and geometry remain entirely Minecraft-owned.
     *
     * Example:
     * {
     *   "frames": ["example:block/furnace/heating_1", ...],
     *   "durations": [1, 1, 1, 1]
     * }
     */
    private static List<VisualVariant> readExplicitAnimation(
            JsonElement element,
            ResourceManager resources,
            String animationName
    ) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Animation '" + animationName + "' must be an object with a 'frames' array"
            );
        }

        JsonObject animation = element.getAsJsonObject();
        List<VisualVariant> variants = new ArrayList<>();

        if (animation.has("frames")) {
            List<Identifier> frames = readExplicitFrameIds(
                    animation.get("frames"), resources, animationName
            );
            List<Long> durations = parseDurations(
                    animation.has("durations") ? animation.get("durations") : null,
                    frames.size()
            );
            variants.add(new VisualVariant(Map.of(), frames, Map.of(), durations));
        }

        if (animation.has("variants")) {
            JsonElement variantsElement = animation.get("variants");
            if (!variantsElement.isJsonArray()) {
                throw new IllegalArgumentException(
                        "'variants' for animation '" + animationName + "' must be an array"
                );
            }

            int variantIndex = 0;
            for (JsonElement variantElement : variantsElement.getAsJsonArray()) {
                variantIndex++;
                if (!variantElement.isJsonObject()) {
                    throw new IllegalArgumentException(
                            "Variant " + variantIndex + " for animation '" + animationName
                                    + "' must be an object"
                    );
                }

                JsonObject variantObject = variantElement.getAsJsonObject();
                Map<String, String> conditions = new HashMap<>();
                if (variantObject.has("when")) {
                    JsonElement whenElement = variantObject.get("when");
                    if (!whenElement.isJsonObject()) {
                        throw new IllegalArgumentException(
                                "'when' for variant " + variantIndex + " of '" + animationName
                                        + "' must be an object"
                        );
                    }
                    for (var condition : whenElement.getAsJsonObject().entrySet()) {
                        if (!condition.getValue().isJsonPrimitive()) {
                            throw new IllegalArgumentException(
                                    "Variant conditions must be string values: " + condition.getKey()
                            );
                        }
                        conditions.put(
                                condition.getKey(),
                                condition.getValue().getAsString().toLowerCase(java.util.Locale.ROOT)
                        );
                    }
                }

                if (!variantObject.has("frames")) {
                    throw new IllegalArgumentException(
                            "Variant " + variantIndex + " for animation '" + animationName
                                    + "' is missing its 'frames' array"
                    );
                }
                List<Identifier> frames = readExplicitFrameIds(
                        variantObject.get("frames"), resources,
                        animationName + " variant " + variantIndex
                );
                List<Long> durations = parseDurations(
                        variantObject.has("durations") ? variantObject.get("durations") : null,
                        frames.size()
                );
                variants.add(new VisualVariant(
                        Map.copyOf(conditions), frames, Map.of(), durations
                ));
            }
        }

        if (variants.isEmpty()) {
            throw new IllegalArgumentException(
                    "Animation '" + animationName + "' must define 'frames' or 'variants'"
            );
        }
        return List.copyOf(variants);
    }

    private static List<Identifier> readExplicitFrameIds(
            JsonElement element,
            ResourceManager resources,
            String description
    ) {
        if (!element.isJsonArray() || element.getAsJsonArray().isEmpty()) {
            throw new IllegalArgumentException(
                    "'frames' for " + description + " must be a non-empty array of model identifiers"
            );
        }

        List<Identifier> frames = new ArrayList<>();
        for (JsonElement frameElement : element.getAsJsonArray()) {
            if (!frameElement.isJsonPrimitive()) {
                throw new IllegalArgumentException(
                        "Every frame for " + description + " must be a model identifier string"
                );
            }
            Identifier modelId = Identifier.parse(frameElement.getAsString());
            Identifier modelResource = Identifier.fromNamespaceAndPath(
                    modelId.getNamespace(), "models/" + modelId.getPath() + ".json"
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

    /**
     * Registers models frame-major for every supported orientation.
     */
    private static List<StandaloneModelKey<BlockStateModel>> registerFrameModels(
            ModelEvent.RegisterStandalone event,
            List<Identifier> frameIds,
            Identifier blockId
    ) {
        boolean button = BuiltInRegistries.BLOCK.getValue(blockId) instanceof ButtonBlock;

        List<ModelOrientation> orientations = new ArrayList<>();
        if (button) {
            for (AttachFace face : AttachFace.values()) {
                for (Direction facing : HORIZONTAL_DIRECTIONS) {
                    orientations.add(new ModelOrientation(face, facing));
                }
            }
        } else {
            orientations.addAll(orientationsForBlock(blockId));
        }

        List<StandaloneModelKey<BlockStateModel>> keys =
                new ArrayList<>(frameIds.size() * orientations.size());

        for (Identifier frameId : frameIds) {
            for (ModelOrientation orientation : orientations) {
                StandaloneModelKey<BlockStateModel> key = new StandaloneModelKey<>(new ModelDebugName() {
                    @Override
                    public String debugName() {
                        String orientationName = orientation.face() == null
                                ? orientation.facing().getName()
                                : orientation.face().getSerializedName() + "/" + orientation.facing().getName();
                        return "stateworks: " + frameId + " [" + orientationName + "]";
                    }
                });

                keys.add(key);
                event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(
                        frameId,
                        button
                                ? ((orientation.face() == AttachFace.FLOOR
                                        || orientation.face() == AttachFace.CEILING)
                                        ? BlockModelRotation.get(
                                                buttonRotation(orientation.face(), orientation.facing())
                                        )
                                        : BlockModelRotation.get(
                                                buttonRotation(orientation.face(), orientation.facing())
                                        ).withUvLock())
                                : BlockModelRotation.get(
                                        rotationFor(orientation.facing(), blockId)
                                )
                ));
            }
        }

        return List.copyOf(keys);
    }

    private static List<ModelOrientation> orientationsForBlock(Identifier blockId) {
        var block = BuiltInRegistries.BLOCK.getValue(blockId);
        BlockState state = block.defaultBlockState();

        List<ModelOrientation> orientations = new ArrayList<>();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            for (Direction direction : Direction.values()) {
                orientations.add(new ModelOrientation(null, direction));
            }
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                orientations.add(new ModelOrientation(null, direction));
            }
        } else {
            // Non-facing blocks keep the historical four-orientation registration
            // so existing animation packs remain compatible.
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                orientations.add(new ModelOrientation(null, direction));
            }
        }

        return orientations;
    }

    private static OctahedralGroup buttonRotation(
            AttachFace face,
            Direction facing
    ) {
        /*
         * Match vanilla button blockstate rotations. The vanilla button model
         * uses the same horizontal Y rotation on the floor and wall, while the
         * ceiling reverses the horizontal rotation after the 180-degree X turn.
         *
         * Wall buttons use UV locking because their side-facing attachment
         * needs the texture orientation preserved. Floor and ceiling buttons
         * intentionally use the raw rotation so their top-facing texture
         * rotates with the model.
         */
        int y = switch (face) {
            case FLOOR, WALL -> switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            case CEILING -> switch (facing) {
                case NORTH -> 180;
                case EAST -> 270;
                case SOUTH -> 0;
                case WEST -> 90;
                default -> 0;
            };
        };

        int x = switch (face) {
            case FLOOR -> 0;
            case WALL -> 90;
            case CEILING -> 180;
        };

        return Quadrant.fromXYAngles(quadrant(x), quadrant(y));
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

    private static OctahedralGroup rotationFor(
            Direction direction,
            Identifier blockId
    ) {
        /*
         * Repeater animation models are authored with SOUTH as their
         * unrotated/base orientation. The Furnace animation models supplied
         * by the example pack are authored with NORTH as their front.
         */
        boolean northBase =
                blockId.equals(Identifier.parse("minecraft:furnace"))
                        || blockId.equals(Identifier.parse("minecraft:blast_furnace"))
                        || blockId.equals(Identifier.parse("minecraft:smoker"));

        if (northBase) {
            return switch (direction) {
                case NORTH -> OctahedralGroup.IDENTITY;
                case EAST -> OctahedralGroup.BLOCK_ROT_Y_90;
                case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_180;
                case WEST -> OctahedralGroup.BLOCK_ROT_Y_270;
                case UP -> OctahedralGroup.BLOCK_ROT_X_90;
                case DOWN -> OctahedralGroup.BLOCK_ROT_X_270;
            };
        }

        return switch (direction) {
            case SOUTH -> OctahedralGroup.IDENTITY;
            case WEST -> OctahedralGroup.BLOCK_ROT_Y_90;
            case NORTH -> OctahedralGroup.BLOCK_ROT_Y_180;
            case EAST -> OctahedralGroup.BLOCK_ROT_Y_270;
            case UP -> OctahedralGroup.BLOCK_ROT_X_90;
            case DOWN -> OctahedralGroup.BLOCK_ROT_X_270;
        };
    }

    public record ModelOrientation(AttachFace face, Direction facing) {
    }

    public static Map<String, List<VisualVariant>> bakedFrames(
            Identifier block,
            StandaloneModelLoader.BakedModels baked
    ) {
        Map<String, List<VisualVariant>> result = new HashMap<>();
        Map<String, List<VisualVariant>> mappings = MAPPINGS.get(block);
        Map<String, List<List<StandaloneModelKey<BlockStateModel>>>> keyMap = KEYS.get(block);
        if (mappings == null || keyMap == null) {
            return result;
        }

        for (var entry : mappings.entrySet()) {
            List<List<StandaloneModelKey<BlockStateModel>>> variantKeys = keyMap.get(entry.getKey());
            if (variantKeys == null || variantKeys.size() != entry.getValue().size()) {
                continue;
            }

            List<VisualVariant> bakedVariants = new ArrayList<>();

            for (int variantIndex = 0; variantIndex < entry.getValue().size(); variantIndex++) {
                VisualVariant definition = entry.getValue().get(variantIndex);
                List<StandaloneModelKey<BlockStateModel>> keys = variantKeys.get(variantIndex);
                boolean button = BuiltInRegistries.BLOCK.getValue(block) instanceof ButtonBlock;
                List<ModelOrientation> orientations = new ArrayList<>();

                if (button) {
                    for (AttachFace face : AttachFace.values()) {
                        for (Direction direction : HORIZONTAL_DIRECTIONS) {
                            orientations.add(new ModelOrientation(face, direction));
                        }
                    }
                } else {
                    orientations.addAll(orientationsForBlock(block));
                }

                if (keys.size() != definition.frameIds().size() * orientations.size()) {
                    continue;
                }

                Map<ModelOrientation, List<BlockStateModel>> directional = new HashMap<>();

                for (int orientationIndex = 0; orientationIndex < orientations.size(); orientationIndex++) {
                    ModelOrientation orientation = orientations.get(orientationIndex);
                    List<BlockStateModel> frames = new ArrayList<>(definition.frameIds().size());

                    for (int frameIndex = 0; frameIndex < definition.frameIds().size(); frameIndex++) {
                        int keyIndex = frameIndex * orientations.size() + orientationIndex;
                        frames.add(baked.get(keys.get(keyIndex)));
                    }

                    if (frames.stream().allMatch(Objects::nonNull)) {
                        directional.put(orientation, List.copyOf(frames));
                    }
                }

                if (directional.size() == orientations.size()) {
                    bakedVariants.add(new VisualVariant(
                            definition.conditions(),
                            definition.frameIds(),
                            Map.copyOf(directional),
                            definition.durations()
                    ));
                }
            }

            if (!bakedVariants.isEmpty()) {
                result.put(entry.getKey(), List.copyOf(bakedVariants));
            }
        }

        return result;
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var models = event.getBakingResult().blockStateModels();
        var bakedStandaloneModels = event.getBakingResult().standaloneModels();

        LOGGER.info("[Stateworks MODEL] ModifyBakingResult: {} block-state model entries", models.size());

        int wrapped = 0;
        for (var entry : new ArrayList<>(models.entrySet())) {
            BlockState state = entry.getKey();
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            Map<String, List<VisualVariant>> frames =
                    bakedFrames(blockId, bakedStandaloneModels);

            if (!frames.isEmpty()) {
                LOGGER.info(
                        "[Stateworks MODEL] Wrapping {} state={} with visual transitions {}",
                        blockId,
                        state,
                        frames.keySet()
                );
                models.put(
                        state,
                        new VirtualStateBlockModel(entry.getValue(), blockId, frames)
                );
                wrapped++;
            }
        }

        LOGGER.info("[Stateworks MODEL] Wrapped {} block-state model entries", wrapped);
    }

    /**
     * A transition's visual frames plus optional vanilla BlockState property
     * conditions. A variant with more conditions wins over a less-specific
     * variant when both match.
     */
    public record VisualVariant(
            Map<String, String> conditions,
            List<Identifier> frameIds,
            Map<ModelOrientation, List<BlockStateModel>> directionalFrames,
            List<Long> durations
    ) {
        public VisualVariant(Map<String, String> conditions, List<Identifier> frameIds) {
            this(conditions, frameIds, Map.of(), List.of());
        }

        public VisualVariant(
                Map<String, String> conditions,
                List<Identifier> frameIds,
                Map<ModelOrientation, List<BlockStateModel>> directionalFrames
        ) {
            this(conditions, frameIds, directionalFrames, List.of());
        }

        public boolean hasExplicitDurations() {
            return !durations.isEmpty();
        }

        public long totalDuration() {
            long total = 0L;
            for (long duration : durations) {
                total = Math.addExact(total, Math.max(1L, duration));
            }
            return total;
        }

        public boolean matches(BlockState state) {
            if (state == null) {
                return conditions.isEmpty();
            }

            for (var entry : conditions.entrySet()) {
                Property<?> property = findProperty(state, entry.getKey());
                if (property == null) {
                    return false;
                }

                String actual = getPropertyName(property, state).toLowerCase(java.util.Locale.ROOT);
                if (!actual.equals(entry.getValue())) {
                    return false;
                }
            }

            return true;
        }

        private static Property<?> findProperty(BlockState state, String name) {
            for (Property<?> property : state.getProperties()) {
                if (property.getName().equals(name)) {
                    return property;
                }
            }
            return null;
        }

        private static String getPropertyName(Property<?> property, BlockState state) {
            return getPropertyNameTyped(property, state);
        }

        private static <T extends Comparable<T>> String getPropertyNameTyped(
                Property<T> property, BlockState state) {
            return property.getName(state.getValue(property));
        }
    }
}
