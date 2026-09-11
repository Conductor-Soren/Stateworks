package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.HashMap;

public final class StateDefinitionLoader
        implements ResourceManagerReloadListener {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "stateworks",
                    "state_definitions"
            );

    public static final StateDefinitionLoader INSTANCE =
            new StateDefinitionLoader();

    private StateDefinitionLoader() {
    }

    @Override
    public void onResourceManagerReload(
            ResourceManager resourceManager
    ) {

        System.out.println("=== STATEWORKS JSON RELOAD ===");

        Map<String, StateDefinition<?>> definitions = new HashMap<>();
        Map<String, StateDefinitionSet<?>> sets = new HashMap<>();
        Map<String, SignalDefinitionSet> signalSets = new HashMap<>();

        Map<Identifier,
                net.minecraft.server.packs.resources.Resource> resources =
                resourceManager.listResources(
                        "states",
                        location -> location.getPath().endsWith(".json")
                );

        for (Map.Entry<Identifier, net.minecraft.server.packs.resources.Resource> entry
                : resources.entrySet()) {

            Identifier location = entry.getKey();

            try (Reader reader = new InputStreamReader(entry.getValue().open())) {

                JsonObject json =
                        JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("signals")) {

                    SignalDefinitionSet signalSet =
                            StateDefinitionParser.parseSignals(json);

                    signalSets.put(signalSet.id(), signalSet);

                    System.out.println(
                            "Registered Signal Definition Set: "
                                    + signalSet.id()
                                    + " | signals=" + signalSet.signals().size()
                                    + " | file=" + location
                    );
                }

                if (json.has("states")) {

                    StateDefinitionSet<?> set =
                            StateDefinitionParser.parseSet(json);

                    sets.put(set.id(), set);

                    System.out.println(
                            "Registered State Definition Set: "
                                    + set.id()
                                    + " | states=" + set.states().size()
                                    + " | default="
                                    + (
                                    set.defaultState() != null
                                            ? set.defaultState().name()
                                            : "<none>"
                            )
                                    + " | file=" + location
                    );

                } else {

                    StateDefinition<?> definition =
                            StateDefinitionParser.parse(json);

                    definitions.put(definition.name(), definition);

                    System.out.println(
                            "Registered State Definition: "
                                    + definition.name()
                                    + " | file=" + location
                    );
                }

            } catch (Exception e) {

                System.err.println(
                        "Failed to load Stateworks definition: "
                                + location
                );

                e.printStackTrace();
            }
        }

        StateRegistry.INSTANCE.replaceAll(
                definitions,
                sets,
                signalSets
        );

        System.out.println("==============================");
    }
}