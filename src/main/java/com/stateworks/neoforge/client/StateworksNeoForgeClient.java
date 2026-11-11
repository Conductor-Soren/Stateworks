package com.stateworks.neoforge.client;

import com.stateworks.neoforge.StateworksNeoForge;
import com.stateworks.minecraft.network.VisualStatePayload;
import com.stateworks.neoforge.client.StateworksVisualOverlay;
import com.stateworks.client.StateworksVisualStateManager;


import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

// This class only loads on the physical client.
@Mod(
        value = StateworksNeoForge.MODID,
        dist = Dist.CLIENT
)
public class StateworksNeoForgeClient {

    public StateworksNeoForgeClient(
            IEventBus modEventBus,
            ModContainer container
    ) {

        StateworksVisualStateManager.setVisualDurationResolver(
                StateworksVisualOverlay::getTransitionDuration
        );

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );

        /*
         * Register all client-side mod-bus events explicitly.
         *
         * This is intentional. In particular, the networking
         * registration must happen on the mod event bus.
         */
        modEventBus.addListener(
                StateworksNeoForgeClient::onClientSetup
        );

        modEventBus.addListener(
                StateworksNeoForgeClient::registerClientPayloads
        );

        modEventBus.addListener(
                StateworksNeoForgeClient::registerBlockStateModels
        );
        modEventBus.addListener(
                StateworksNeoForgeClient::registerStandaloneModels
        );
        modEventBus.addListener(
                StateworksNeoForgeClient::modifyBakingResult
        );

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                StateworksNeoForgeClient::onClientTick
        );
    }

    private static void onClientSetup(
            FMLClientSetupEvent event
    ) {

        StateworksNeoForge.LOGGER.info(
                "HELLO FROM CLIENT SETUP"
        );

        StateworksNeoForge.LOGGER.info(
                "MINECRAFT NAME >> {}",
                Minecraft.getInstance()
                        .getUser()
                        .getName()
        );
    }

    private static void onClientTick(
            ClientTickEvent.Post event
    ) {
        StateworksVisualStateManager.tick();
    }

    private static void registerBlockStateModels(
            RegisterBlockStateModels event
    ) {
        // Stateworks no longer requires a custom blockstate type for vanilla blocks.
    }

    private static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        if (Minecraft.getInstance().getResourceManager() != null) {
            StateworksVisualOverlay.registerStandaloneModels(
                    event,
                    Minecraft.getInstance().getResourceManager()
            );
        }
    }

    private static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        StateworksNeoForge.LOGGER.info("[Stateworks MODEL] Client received ModifyBakingResult event");
        StateworksVisualOverlay.modifyBakingResult(event);
    }

    private static void registerClientPayloads(
            RegisterClientPayloadHandlersEvent event
    ) {

        StateworksNeoForge.LOGGER.info(
                "STATEWORKS: Registering visual_state client handler"
        );

        event.register(
                VisualStatePayload.TYPE,
                (payload, context) -> context.enqueueWork(() -> {
                    StateworksNeoForge.LOGGER.debug(
                            "Stateworks received visual update: pos={} set={} state={}",
                            payload.position(),
                            payload.stateSetId(),
                            payload.stateName()
                    );

                    var player = context.player();
                    if (player == null || player.level() == null) {
                        return;
                    }

                    StateworksVisualStateManager.set(
                            player.level().dimension(),
                            payload.position(),
                            payload.stateSetId(),
                            payload.stateName(),
                            payload.previousStateName(),
                            payload.visualTransitionName(),
                            payload.transitionElapsed(),
                            payload.transitionDuration(),
                            payload.previousSignals(),
                            payload.signals()
                    );

                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.levelRenderer != null) {
                        minecraft.levelExtractor.setBlocksDirty(
                                payload.position().getX(),
                                payload.position().getY(),
                                payload.position().getZ(),
                                payload.position().getX(),
                                payload.position().getY(),
                                payload.position().getZ()
                        );
                    }
                })
        );
    }
}