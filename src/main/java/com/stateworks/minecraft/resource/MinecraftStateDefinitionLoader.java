package com.stateworks.minecraft.resource;

import com.stateworks.core.definition.ParsedStateDefinitions;
import com.stateworks.core.definition.StateDefinitionParser;
import com.stateworks.core.registry.StateRegistry;
import com.stateworks.signal.SignalDefinitionSet;
import com.stateworks.state.StateDefinition;
import com.stateworks.state.StateDefinitionSet;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * NeoForge/Minecraft resource adapter. It discovers JSON resources and passes
 * their text to the Minecraft-independent StateDefinitionParser.
 */
public final class MinecraftStateDefinitionLoader implements ResourceManagerReloadListener {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            "stateworks",
            "state_definitions"
    );

    public static final MinecraftStateDefinitionLoader INSTANCE =
            new MinecraftStateDefinitionLoader();

    private MinecraftStateDefinitionLoader() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        System.out.println("=== STATEWORKS JSON RELOAD ===");

        Map<String, StateDefinition<?>> definitions = new HashMap<>();
        Map<String, StateDefinitionSet<?>> sets = new HashMap<>();
        Map<String, SignalDefinitionSet> signalSets = new HashMap<>();

        Map<Identifier, Resource> resources = resourceManager.listResources(
                "states",
                location -> location.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier location = entry.getKey();

            try (Reader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8
            )) {
                StringBuilder jsonText = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    jsonText.append(buffer, 0, read);
                }

                ParsedStateDefinitions parsed =
                        StateDefinitionParser.parseDocument(jsonText.toString());

                definitions.putAll(parsed.definitions());
                sets.putAll(parsed.stateSets());
                signalSets.putAll(parsed.signalSets());

                System.out.println(
                        "Parsed Stateworks resource: " + location
                                + " | definitions=" + parsed.definitions().size()
                                + " | stateSets=" + parsed.stateSets().size()
                                + " | signalSets=" + parsed.signalSets().size()
                );
            } catch (Exception exception) {
                System.err.println("Failed to load Stateworks definition: " + location);
                exception.printStackTrace();
            }
        }

        StateRegistry.INSTANCE.replaceDataDriven(definitions, sets, signalSets);
        System.out.println(
                "Stateworks resource reload complete | definitions=" + definitions.size()
                        + " | stateSets=" + sets.size()
                        + " | signalSets=" + signalSets.size()
        );
    }
}
