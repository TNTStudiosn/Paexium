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
import com.TNTStudios.paexium.commands.Ganador;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

@Environment(EnvType.CLIENT)
public class PaexiumClient implements ClientModInitializer {

    private static final Identifier MEDALLA_TEXTURE = new Identifier("paexium", "textures/gui/medalla.png");
    private static boolean mostrarMedalla = false;
    private static String ganadorNombre = "";
    private static String mensajePersonalizado = "";
    private static int ticksMostrarMedalla = 0;
    private static final int DURACION_TOTAL = 400; // 20 segundos
    private static final int TRANSICION_FADE = 60; // 3 segundos



    @Override
    public void onInitializeClient() {
        DiscordPresenceHandler.init();
        ArmorRenderer.register(new CascoArmorRenderer(), PaexiumItems.CASCO);
        ClientTickEvents.END_CLIENT_TICK.register(client -> DiscordPresenceHandler.tick());

        // Registrar handler
        ClientPlayNetworking.registerGlobalReceiver(RuletaNetworking.RULETA_PACKET_ID, (client, handler, buf, responseSender) -> {
            int opcion = buf.readInt();
            long startTick = buf.readLong();

            client.execute(() -> {
                RuletaOverlay.startSpin(opcion, startTick);
            });
        });

        HudRenderCallback.EVENT.register(new RuletaOverlay());

        // Registrar el paquete de medalla
        ClientPlayNetworking.registerGlobalReceiver(Ganador.MEDALLA_PACKET, (client, handler, buf, responseSender) -> {
            String nombre = buf.readString();
            String mensaje = buf.readString();
            client.execute(() -> {
                ganadorNombre = nombre;
                mensajePersonalizado = mensaje;
                mostrarMedalla = true;
                ticksMostrarMedalla = 0;
            });
        });

        // Registrar renderizado de HUD
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (mostrarMedalla) {
                renderMedalla(drawContext, MinecraftClient.getInstance());
            }
        });

    }

    private void renderMedalla(DrawContext drawContext, MinecraftClient client) {
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();

        ticksMostrarMedalla++;
        if (ticksMostrarMedalla > DURACION_TOTAL) { // Mostrar la medalla por la duración especificada
            mostrarMedalla = false;
            return;
        }

        // Calcular opacidad para fade in y fade out
        float alpha;
        if (ticksMostrarMedalla <= TRANSICION_FADE) {
            alpha = (float) ticksMostrarMedalla / TRANSICION_FADE; // Fade in
        } else if (ticksMostrarMedalla >= DURACION_TOTAL - TRANSICION_FADE) {
            alpha = (float) (DURACION_TOTAL - ticksMostrarMedalla) / TRANSICION_FADE; // Fade out
        } else {
            alpha = 1.0f; // Mantener opacidad máxima
        }

        // Dibujar medalla con opacidad
        drawContext.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        drawContext.drawTexture(MEDALLA_TEXTURE, (scaledWidth / 2) - 64, (scaledHeight / 2) - 64, 0, 0, 128, 128, 128, 128);

        // Dibujar texto centrado
        String textoCompleto = "§6" + ganadorNombre + " " + mensajePersonalizado;
        int textoWidth = client.textRenderer.getWidth(textoCompleto);
        int centerX = scaledWidth / 2;

        // Calcular posición horizontal centrada
        drawContext.drawTextWithShadow(client.textRenderer, textoCompleto, centerX - (textoWidth / 2), (scaledHeight / 2) + 80, 0xFFFFFF);
    }




}
