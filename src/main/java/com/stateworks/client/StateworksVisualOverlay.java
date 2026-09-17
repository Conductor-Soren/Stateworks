package com.stateworks.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stateworks.client.model.VirtualStateBlockModel;
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
        // Legacy animations must continue to use their registered logical
        // transition duration.
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
                JsonObject states = root.getAsJsonObject("states");

                Map<String, List<VisualVariant>> stateVariants = new HashMap<>();
                Map<String, List<List<StandaloneModelKey<BlockStateModel>>>> stateKeys = new HashMap<>();

                for (var stateEntry : states.entrySet()) {
                    List<VisualVariant> variants = readVariants(stateEntry.getValue(), resources);
                    if (variants.isEmpty()) {
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

    private static List<VisualVariant> readVariants(JsonElement element, ResourceManager resources) {
        List<VisualVariant> variants = new ArrayList<>();

        if (element.isJsonPrimitive()) {
            Identifier base = Identifier.parse(element.getAsString());
            LOGGER.debug("[Stateworks] Loading visual state -> {}", base);
            return discoverVariants(resources, base);
        }

        if (!element.isJsonObject()) return variants;
        JsonObject object = element.getAsJsonObject();

        // Preferred format: "Heating": "example:block/furnace/heating".
        // Frames are discovered automatically as heating_1, heating_2, ...
        for (var entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) continue;
            Identifier base = Identifier.parse(value.getAsString());
            LOGGER.debug("[Stateworks] Loading visual state {} -> {}", entry.getKey(), base);

            // Discover either a simple sequence (<base>_1.json, ...) or
            // property variants such as repeater:
            // <base>_1_frame_1.json, <base>_1_frame_2.json, ...
            List<VisualVariant> discovered = discoverVariants(resources, base);
            if (!discovered.isEmpty()) {
                RESOURCE_TRANSITIONS.put(entry.getKey(), List.copyOf(discovered));
                LOGGER.info(
                        "[Stateworks] Registered visual transition '{}' with {} variant(s)",
                        entry.getKey(),
                        discovered.size()
                );
                variants.addAll(discovered);
            }

        }

        // Explicit single-state format:
        // {
        //   "model": "namespace:block/path/base",
        //   "durations": [1, 2, 1, ...]
        // }
        //
        // The durations array contains one duration per discovered frame.
        // It must be preserved as frame timing, not interpreted as a single
        // transition duration.
        if (object.has("model") && object.get("model").isJsonPrimitive()) {
            Identifier base = Identifier.parse(object.get("model").getAsString());
            List<Identifier> frames = discoverFrames(resources, base);
            List<Long> durations = parseDurations(
                    object.has("durations") ? object.get("durations") : null,
                    frames.size()
            );
            if (!frames.isEmpty()) {
                variants.add(new VisualVariant(
                        Map.of(),
                        frames,
                        Map.of(),
                        durations
                ));
            }
        }

        // Backwards-compatible numbered format.
        for (var entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonObject()) continue;
            List<Identifier> frames = readFrameIds(value.getAsJsonObject());
            if (!frames.isEmpty()) variants.add(new VisualVariant(Map.of(), frames));
        }

        if (object.has("frames") && object.get("frames").isJsonObject()) {
            List<Identifier> frames = readFrameIds(object.getAsJsonObject("frames"));
            if (!frames.isEmpty()) variants.add(new VisualVariant(Map.of(), frames));
        }

        if (object.has("variants") && object.get("variants").isJsonArray()) {
            for (JsonElement variantElement : object.getAsJsonArray("variants")) {
                if (!variantElement.isJsonObject()) continue;
                JsonObject variantObject = variantElement.getAsJsonObject();
                JsonObject whenObject = variantObject.has("when") && variantObject.get("when").isJsonObject()
                        ? variantObject.getAsJsonObject("when") : new JsonObject();
                Map<String, String> conditions = new HashMap<>();
                for (var condition : whenObject.entrySet()) {
                    if (condition.getValue().isJsonPrimitive()) {
                        conditions.put(condition.getKey(), condition.getValue().getAsString().toLowerCase(java.util.Locale.ROOT));
                    }
                }
                List<Identifier> frames = List.of();
                if (variantObject.has("frames") && variantObject.get("frames").isJsonObject()) {
                    frames = readFrameIds(variantObject.getAsJsonObject("frames"));
                } else if (variantObject.has("model") && variantObject.get("model").isJsonPrimitive()) {
                    frames = discoverFrames(resources, Identifier.parse(variantObject.get("model").getAsString()));
                }

                List<Long> durations = parseDurations(
                        variantObject.has("durations") ? variantObject.get("durations") : null,
                        frames.size()
                );

                if (!frames.isEmpty()) {
                    variants.add(new VisualVariant(
                            Map.copyOf(conditions),
                            frames,
                            Map.of(),
                            durations
                    ));
                }
            }
        }
        return variants;
    }

    private static List<Identifier> readFrameIds(JsonObject frames) {
        List<Identifier> ids = new ArrayList<>();
        for (int frame = 1; ; frame++) {
            String key = Integer.toString(frame);
            if (!frames.has(key)) break;
            ids.add(Identifier.parse(frames.get(key).getAsString()));
        }
        return ids.isEmpty() ? List.of() : List.copyOf(ids);
    }

    private static List<Long> parseDurations(JsonElement element, int frameCount) {
        if (frameCount <= 0) return List.of();

        List<Long> durations = new ArrayList<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (!value.isJsonPrimitive()) break;
                try {
                    durations.add(Math.max(1L, value.getAsLong()));
                } catch (RuntimeException ignored) {
                    break;
                }
            }
        }

        while (durations.size() < frameCount) durations.add(1L);
        if (durations.size() > frameCount) {
            durations = new ArrayList<>(durations.subList(0, frameCount));
        }
        return List.copyOf(durations);
    }

    private static List<VisualVariant> discoverVariants(ResourceManager resources, Identifier base) {
        /*
         * Property-frame animations use:
         *
         *   <base>_<delay>_frame_<frame>.json
         *
         * Do not depend on ResourceManager.listResources() here. In newer
         * Minecraft/NeoForge versions the resource IDs supplied to that
         * predicate can differ from the fully-qualified path expected by the
         * old implementation. Direct getResource() lookups are deterministic
         * and also make missing frames easy to diagnose.
         */
        List<VisualVariant> propertyVariants = new ArrayList<>();

        for (int variant = 1; variant <= 16; variant++) {
            List<Identifier> frames = new ArrayList<>();

            for (int frame = 1; frame <= 16; frame++) {
                String modelPath =
                        base.getPath()
                                + "_" + variant
                                + "_frame_" + frame;

                Identifier modelId = Identifier.parse(
                        base.getNamespace() + ":" + modelPath
                );

                Identifier resourceId = Identifier.parse(
                        base.getNamespace() + ":models/" + modelPath + ".json"
                );

                if (resources.getResource(resourceId).isEmpty()) {
                    break;
                }

                frames.add(modelId);
            }

            if (frames.isEmpty()) {
                /*
                 * Variants are numbered consecutively, so the first missing
                 * variant ends the property-frame sequence.
                 */
                if (variant > 1) {
                    break;
                }
                continue;
            }

            propertyVariants.add(
                    new VisualVariant(
                            Map.of(
                                    "delay",
                                    Integer.toString(variant)
                            ),
                            List.copyOf(frames)
                    )
            );

            LOGGER.info(
                    "[Stateworks] Discovered {} frame(s) for {} with delay={}",
                    frames.size(),
                    base,
                    variant
            );
        }

        if (!propertyVariants.isEmpty()) {
            return List.copyOf(propertyVariants);
        }

        List<Identifier> frames = discoverFrames(resources, base);
        return frames.isEmpty()
                ? List.of()
                : List.of(new VisualVariant(Map.of(), frames));
    }

    /** Discovers <base>_1, <base>_2, ... until the first missing model. */
    private static List<Identifier> discoverFrames(ResourceManager resources, Identifier base) {
        List<Identifier> frames = new ArrayList<>();
        for (int frame = 1; ; frame++) {
            Identifier modelId = Identifier.parse(base.getNamespace() + ":" + base.getPath() + "_" + frame);
            Identifier resourceId = Identifier.parse(base.getNamespace() + ":models/" + base.getPath() + "_" + frame + ".json");
            if (resources.getResource(resourceId).isEmpty()) break;
            frames.add(modelId);
            LOGGER.debug("[Stateworks] Discovered visual frame {} for base {}", modelId, base);
        }
        if (!frames.isEmpty()) {
            LOGGER.info("[Stateworks] Discovered {} visual frame(s) for {}", frames.size(), base);
        }
        return frames.isEmpty() ? List.of() : List.copyOf(frames);
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
