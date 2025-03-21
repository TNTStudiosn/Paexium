package com.TNTStudios.paexium.client;

import com.TNTStudios.paexium.client.discord.DiscordPresenceHandler;
import com.TNTStudios.paexium.client.armor.CascoArmorRenderer;
import com.TNTStudios.paexium.client.ruleta.RuletaOverlay;
import com.TNTStudios.paexium.items.PaexiumItems;
import com.TNTStudios.paexium.network.RuletaNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

@Environment(EnvType.CLIENT)
public class PaexiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DiscordPresenceHandler.init();
        ArmorRenderer.register(new CascoArmorRenderer(), PaexiumItems.CASCO);
        ClientTickEvents.END_CLIENT_TICK.register(client -> DiscordPresenceHandler.tick());

        // Registrar handler
        ClientPlayNetworking.registerGlobalReceiver(RuletaNetworking.RULETA_PACKET_ID, (client, handler, buf, responseSender) -> {
            int opcion = buf.readInt();
            long startTick = buf.readLong(); // nuevo

            client.execute(() -> {
                // Arrancamos la ruleta con la opción y el tick de inicio
                RuletaOverlay.startSpin(opcion, startTick);
            });
        });

        HudRenderCallback.EVENT.register(new RuletaOverlay());
    }


}
