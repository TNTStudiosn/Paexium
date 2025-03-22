package com.TNTStudios.paexium.commands;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class VerParcelaCommand {

    private static final File file = new File("config/paexium/asignaciones.json");
    private static final Gson gson = new Gson();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(CommandManager.literal("verparcela")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        MinecraftServer server = source.getServer();

                        Map<UUID, Integer> asignaciones = cargarAsignaciones();
                        if (asignaciones == null || asignaciones.isEmpty()) {
                            source.sendFeedback(() -> Text.literal("📭 No hay asignaciones registradas."), false);
                            return 0;
                        }

                        source.sendFeedback(() -> Text.literal("📋 Asignaciones actuales:"), false);

                        for (Map.Entry<UUID, Integer> entry : asignaciones.entrySet()) {
                            UUID uuid = entry.getKey();
                            int parcela = entry.getValue();
                            String nombre = obtenerNombreDesdeUUID(server, uuid);
                            source.sendFeedback(() ->
                                    Text.literal("▪ " + nombre + " → Parcela " + parcela), false);
                        }

                        return 1;
                    }));
        });
    }

    private static Map<UUID, Integer> cargarAsignaciones() {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<UUID, Integer>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String obtenerNombreDesdeUUID(MinecraftServer server, UUID uuid) {
        // Intenta encontrar al jugador online
        var player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }

        // Si no está online, intenta con su caché (last known name)
        var profile = server.getUserCache().getByUuid(uuid);
        return profile.map(gameProfile -> gameProfile.getName()).orElse("Desconocido (" + uuid + ")");
    }
}
