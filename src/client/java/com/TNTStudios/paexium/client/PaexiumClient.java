package com.TNTStudios.paexium.client;

import com.TNTStudios.paexium.client.discord.DiscordPresenceHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class PaexiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DiscordPresenceHandler.init();

        // Ejecutar el callback en cada tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> DiscordPresenceHandler.tick());
    }
}
