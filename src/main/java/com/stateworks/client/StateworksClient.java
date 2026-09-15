package com.stateworks.client;

import com.stateworks.Stateworks;
import com.stateworks.network.StateworksNetwork;

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
        value = Stateworks.MODID,
        dist = Dist.CLIENT
)
public class StateworksClient {

    public StateworksClient(
            IEventBus modEventBus,
            ModContainer container
    ) {

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
                StateworksClient::onClientSetup
        );

        modEventBus.addListener(
                StateworksClient::registerClientPayloads
        );

        modEventBus.addListener(
                StateworksClient::registerBlockStateModels
        );
        modEventBus.addListener(
                StateworksClient::registerStandaloneModels
        );
        modEventBus.addListener(
                StateworksClient::modifyBakingResult
        );

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                StateworksClient::onClientTick
        );
    }

    private static void onClientSetup(
            FMLClientSetupEvent event
    ) {

        Stateworks.LOGGER.info(
                "HELLO FROM CLIENT SETUP"
        );

        Stateworks.LOGGER.info(
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
        Stateworks.LOGGER.info("[Stateworks MODEL] Client received ModifyBakingResult event");
        StateworksVisualOverlay.modifyBakingResult(event);
    }

    private static void registerClientPayloads(
            RegisterClientPayloadHandlersEvent event
    ) {

        Stateworks.LOGGER.info(
                "STATEWORKS: Registering visual_state client handler"
        );

        StateworksNetwork.registerClient(
                event
        );
    }
}