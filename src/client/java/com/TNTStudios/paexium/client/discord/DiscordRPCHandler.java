package com.TNTStudios.paexium.client.discord;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class DiscordRPCHandler {

    private static final String APPLICATION_ID = "1325598588905979964"; // <-- reemplaza con tu App ID
    private static final DiscordRPC INSTANCE = DiscordRPC.INSTANCE;
    private static final DiscordRichPresence presence = new DiscordRichPresence();

    public static void start() {
        INSTANCE.Discord_Initialize(APPLICATION_ID, null, true, null);

        presence.state = "Jugando";
        presence.details = "Una ciudad en ruinas, un desafío sin precedentes y solo los mejores constructores podrán devolverle la vida.";
        presence.startTimestamp = System.currentTimeMillis() / 1000L;
        presence.largeImageKey = "loo";
        presence.largeImageText = "Evento de Minecraft";
        presence.smallImageKey = "iconoo";
        presence.smallImageText = "TNTStudios";

        INSTANCE.Discord_UpdatePresence(presence);
    }

    public static void shutdown() {
        INSTANCE.Discord_Shutdown();
    }

    public interface DiscordEventHandlers extends Library {
        void ready();
    }

    public interface DiscordRPC extends Library {
        DiscordRPC INSTANCE = Native.load("discord-rpc", DiscordRPC.class);
        void Discord_Initialize(String applicationId, Pointer handlers, boolean autoRegister, String optionalSteamId);
        void Discord_Shutdown();
        void Discord_RunCallbacks();
        void Discord_UpdatePresence(DiscordRichPresence presence);
    }

    public static class DiscordRichPresence extends com.sun.jna.Structure {
        public String state;
        public String details;
        public long startTimestamp;
        public long endTimestamp;
        public String largeImageKey;
        public String largeImageText;
        public String smallImageKey;
        public String smallImageText;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("state", "details", "startTimestamp", "endTimestamp",
                    "largeImageKey", "largeImageText", "smallImageKey", "smallImageText");
        }
    }
}
