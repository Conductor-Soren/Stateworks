package com.stateworks.fabric;

import com.stateworks.condition.BuiltinConditions;
import com.stateworks.core.StateworksConstants;
import com.stateworks.minecraft.network.VisualStatePayload;
import com.stateworks.minecraft.transition.MinecraftStateMachineService;
import com.stateworks.minecraft.state.BuiltinStateSets;
import com.stateworks.fabric.event.FabricServerEvents;
import com.stateworks.fabric.network.FabricStateworksNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateworksFabric implements ModInitializer {
    public static final String MOD_ID = StateworksConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Stateworks Fabric adapter");

        BuiltinConditions.register();
        BuiltinStateSets.register();

        PayloadTypeRegistry.clientboundPlay().register(
                VisualStatePayload.TYPE,
                VisualStatePayload.STREAM_CODEC
        );
        MinecraftStateMachineService.installTransport(
                FabricStateworksNetwork::sendToLevel
        );

        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "state_definitions"),
                com.stateworks.minecraft.resource.MinecraftStateDefinitionLoader.INSTANCE
        );

        FabricServerEvents.register();
    }
}
