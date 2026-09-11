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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
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

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event, ResourceManager resources) {
        MAPPINGS.clear();
        KEYS.clear();

        for (var entry : resources.listResources("stateworks/visuals", id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Identifier block = Identifier.parse(root.get("block").getAsString());
                JsonObject states = root.getAsJsonObject("states");

                Map<String, List<VisualVariant>> stateVariants = new HashMap<>();
                Map<String, List<List<StandaloneModelKey<BlockStateModel>>>> stateKeys = new HashMap<>();

                for (var stateEntry : states.entrySet()) {
                    List<VisualVariant> variants = readVariants(stateEntry.getValue());
                    if (variants.isEmpty()) {
                        continue;
                    }

                    stateVariants.put(stateEntry.getKey(), List.copyOf(variants));

                    List<List<StandaloneModelKey<BlockStateModel>>> variantKeys = new ArrayList<>();
                    for (VisualVariant variant : variants) {
                        variantKeys.add(registerFrameModels(event, variant.frameIds()));
                    }
                    stateKeys.put(stateEntry.getKey(), List.copyOf(variantKeys));
                }

                if (!stateVariants.isEmpty()) {
                    MAPPINGS.put(block, Map.copyOf(stateVariants));
                    KEYS.put(block, Map.copyOf(stateKeys));
                }
            } catch (Exception ignored) {
                // Invalid optional Stateworks visual files are ignored.
            }
        }
    }

    private static List<VisualVariant> readVariants(JsonElement element) {
        List<VisualVariant> variants = new ArrayList<>();

        if (!element.isJsonObject()) {
            return variants;
        }

        JsonObject object = element.getAsJsonObject();

        // Backwards-compatible format:
        // "opening": { "1": "...", "2": "..." }
        List<Identifier> directFrames = readFrameIds(object);
        if (!directFrames.isEmpty()) {
            variants.add(new VisualVariant(Map.of(), directFrames));
        }

        // Variant-aware format:
        // "opening": {
        //   "frames": { "1": "...", ... },
        //   "variants": [
        //     { "when": { "hinge": "left" }, "frames": { ... } }
        //   ]
        // }
        if (object.has("frames") && object.get("frames").isJsonObject()) {
            List<Identifier> frames = readFrameIds(object.getAsJsonObject("frames"));
            if (!frames.isEmpty()) {
                variants.add(new VisualVariant(Map.of(), frames));
            }
        }

        if (object.has("variants") && object.get("variants").isJsonArray()) {
            for (JsonElement variantElement : object.getAsJsonArray("variants")) {
                if (!variantElement.isJsonObject()) {
                    continue;
                }

                JsonObject variantObject = variantElement.getAsJsonObject();
                JsonObject whenObject = variantObject.has("when")
                        && variantObject.get("when").isJsonObject()
                        ? variantObject.getAsJsonObject("when")
                        : new JsonObject();

                JsonObject framesObject = variantObject.has("frames")
                        && variantObject.get("frames").isJsonObject()
                        ? variantObject.getAsJsonObject("frames")
                        : null;

                if (framesObject == null) {
                    continue;
                }

                List<Identifier> frames = readFrameIds(framesObject);
                if (frames.isEmpty()) {
                    continue;
                }

                Map<String, String> conditions = new HashMap<>();
                for (var condition : whenObject.entrySet()) {
                    if (condition.getValue().isJsonPrimitive()) {
                        conditions.put(
                                condition.getKey(),
                                condition.getValue().getAsString().toLowerCase(java.util.Locale.ROOT)
                        );
                    }
                }

                variants.add(new VisualVariant(Map.copyOf(conditions), frames));
            }
        }

        return variants;
    }

    private static List<Identifier> readFrameIds(JsonObject frames) {
        List<Identifier> ids = new ArrayList<>();
        for (int frame = 1; ; frame++) {
            String key = Integer.toString(frame);
            if (!frames.has(key)) {
                break;
            }
            ids.add(Identifier.parse(frames.get(key).getAsString()));
        }

        // Frame numbers must be contiguous starting at 1. Any non-numeric
        // keys or gaps are ignored by this simple format.
        if (ids.isEmpty()) {
            return List.of();
        }
        return ids;
    }

    /**
     * Registers models frame-major: frame 1's four directions, then frame 2,
     * etc. Keeping this ordering explicit prevents the transition frame from
     * accidentally changing orientation between ticks.
     */
    private static List<StandaloneModelKey<BlockStateModel>> registerFrameModels(
            ModelEvent.RegisterStandalone event,
            List<Identifier> frameIds
    ) {
        Direction[] directions = HORIZONTAL_DIRECTIONS;
        List<StandaloneModelKey<BlockStateModel>> keys =
                new ArrayList<>(frameIds.size() * directions.length);

        for (Identifier frameId : frameIds) {
            for (Direction direction : directions) {
                StandaloneModelKey<BlockStateModel> key = new StandaloneModelKey<>(new ModelDebugName() {
                    @Override
                    public String debugName() {
                        return "stateworks: " + frameId + " [" + direction.getName() + "]";
                    }
                });

                keys.add(key);
                event.register(key, SimpleUnbakedStandaloneModel.blockStateModel(
                        frameId,
                        BlockModelRotation.get(rotationFor(direction))
                ));
            }
        }

        return List.copyOf(keys);
    }

    private static OctahedralGroup rotationFor(Direction direction) {
        return switch (direction) {
            case SOUTH -> OctahedralGroup.BLOCK_ROT_Y_180;
            case WEST -> OctahedralGroup.BLOCK_ROT_Y_270;
            case NORTH -> OctahedralGroup.IDENTITY;
            case EAST -> OctahedralGroup.BLOCK_ROT_Y_90;
            case UP -> OctahedralGroup.BLOCK_ROT_X_90;
            case DOWN -> OctahedralGroup.BLOCK_ROT_X_270;
        };
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
                Direction[] directions = HORIZONTAL_DIRECTIONS;

                if (keys.size() != definition.frameIds().size() * directions.length) {
                    continue;
                }

                Map<Direction, List<BlockStateModel>> directional =
                        new EnumMap<>(Direction.class);

                for (int directionIndex = 0; directionIndex < directions.length; directionIndex++) {
                    Direction direction = directions[directionIndex];
                    List<BlockStateModel> frames = new ArrayList<>(definition.frameIds().size());

                    for (int frameIndex = 0; frameIndex < definition.frameIds().size(); frameIndex++) {
                        int keyIndex = frameIndex * directions.length + directionIndex;
                        frames.add(baked.get(keys.get(keyIndex)));
                    }

                    if (frames.stream().allMatch(Objects::nonNull)) {
                        directional.put(direction, List.copyOf(frames));
                    }
                }

                if (directional.size() == directions.length) {
                    bakedVariants.add(new VisualVariant(
                            definition.conditions(),
                            definition.frameIds(),
                            Map.copyOf(directional)
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

        for (var entry : new ArrayList<>(models.entrySet())) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(entry.getKey().getBlock());
            Map<String, List<VisualVariant>> frames =
                    bakedFrames(blockId, bakedStandaloneModels);

            if (!frames.isEmpty()) {
                models.put(
                        entry.getKey(),
                        new VirtualStateBlockModel(entry.getValue(), blockId, frames)
                );
            }
        }
    }

    /**
     * A transition's visual frames plus optional vanilla BlockState property
     * conditions. A variant with more conditions wins over a less-specific
     * variant when both match.
     */
    public record VisualVariant(
            Map<String, String> conditions,
            List<Identifier> frameIds,
            Map<Direction, List<BlockStateModel>> directionalFrames
    ) {
        public VisualVariant(Map<String, String> conditions, List<Identifier> frameIds) {
            this(conditions, frameIds, Map.of());
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
