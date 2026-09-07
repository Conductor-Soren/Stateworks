package com.stateworks.client;

import com.stateworks.Stateworks;
import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.block.*;

import com.stateworks.network.StateworksNetwork;
import com.stateworks.client.StateworksVisualStateManager;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import com.stateworks.client.model.VirtualStateBlockModel;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;

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
                StateworksClient::onRegisterReloadListeners
        );

        modEventBus.addListener(
                StateworksClient::registerClientPayloads
        );

        modEventBus.addListener(
                StateworksClient::registerBlockStateModels
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

    private static void onRegisterReloadListeners(
            AddClientReloadListenersEvent event
    ) {

        event.addListener(
                StateDefinitionLoader.ID,
                StateDefinitionLoader.INSTANCE
        );

        /*
         * State definitions provide visual model dependencies used by
         * VirtualStateBlockModel. Model discovery runs as its own reload
         * listener, so explicitly order our state-definition load before
         * vanilla's model reload.
         */
        event.addDependency(
                StateDefinitionLoader.ID,
                VanillaClientListeners.MODELS
        );
    }

    private static void registerBlockStateModels(
            RegisterBlockStateModels event
    ) {
        event.registerModel(
                VirtualStateBlockModel.Unbaked.ID,
                VirtualStateBlockModel.Unbaked.CODEC
        );
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