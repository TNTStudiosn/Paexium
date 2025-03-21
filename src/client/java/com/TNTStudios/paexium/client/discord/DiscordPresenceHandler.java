package com.TNTStudios.paexium.client.discord;

import com.hypherionmc.craterlib.core.rpcsdk.DiscordRichPresence;
import com.hypherionmc.craterlib.core.rpcsdk.DiscordRPC;
import com.hypherionmc.craterlib.core.rpcsdk.helpers.RPCButton;

public class DiscordPresenceHandler {

    private static boolean initialized = false;
    private static final String APP_ID = "1325598588905979964";

    @SuppressWarnings("removal")
    public static void init() {
        if (initialized) return;

        DiscordRPC.INSTANCE.Discord_Initialize(APP_ID, null, true, null);
        updatePresence();
        initialized = true;
    }

    @SuppressWarnings("removal")
    public static void updatePresence() {
        DiscordRichPresence presence = new DiscordRichPresence();

        // 🏗️ Añadir espacio entre detalles y estado usando \n
        presence.details = "🏗️Una ciudad en ruinas, un desafío sin precedentes y solo los mejores constructores podrán devolverle la vida.🏙️";
        presence.state = "🌌𝗗𝗜𝗠𝗘𝗡𝗦𝗜𝗢́𝗡 𝗔𝗣𝗥𝗢𝗕𝗔𝗗𝗔 𝗣𝗢𝗥 𝗟𝗔 𝗔𝗖𝗠🌌";
        presence.startTimestamp = System.currentTimeMillis() / 1000L;
        presence.largeImageKey = "loo";
        presence.largeImageText = "🌟Evento de Minecraft🌟";
        presence.smallImageKey = "iconoo";
        presence.smallImageText = "🚀TNTStudios🚀";

        // 🔥 Crear y asignar los botones correctamente
        RPCButton button1 = RPCButton.create("🔥 Mejor Host 🔥", "https://www.holy.gg/");
        RPCButton button2 = RPCButton.create("⚡ TNTStudios ⚡", "https://tntstudiosn.space/");

        presence.button_label_1 = button1.getLabel();
        presence.button_url_1 = button1.getUrl();
        presence.button_label_2 = button2.getLabel();
        presence.button_url_2 = button2.getUrl();

        DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);
    }

    @SuppressWarnings("removal")
    public static void tick() {
        DiscordRPC.INSTANCE.Discord_RunCallbacks();
    }

    @SuppressWarnings("removal")
    public static void shutdown() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
    }
}
