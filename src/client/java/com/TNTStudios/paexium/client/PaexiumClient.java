package com.TNTStudios.paexium.client;

import net.fabricmc.api.ClientModInitializer;
import com.TNTStudios.paexium.client.discord.DiscordRPCHandler;

public class PaexiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DiscordRPCHandler.start();
    }
}
