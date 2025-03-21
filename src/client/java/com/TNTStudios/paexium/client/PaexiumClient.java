package com.TNTStudios.paexium.client;

import com.TNTStudios.paexium.client.discord.DiscordPresenceHandler;
import com.TNTStudios.paexium.client.armor.CascoArmorRenderer;
import com.TNTStudios.paexium.items.PaexiumItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class PaexiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //registro discord
        DiscordPresenceHandler.init();

        // Registrar renderizado de casco personalizado
        ArmorRenderer.register(new CascoArmorRenderer(), PaexiumItems.CASCO);

        ClientTickEvents.END_CLIENT_TICK.register(client -> DiscordPresenceHandler.tick());
    }

}
