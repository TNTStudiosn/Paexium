package com.TNTStudios.paexium.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public class DescalificarCommand {

    private static final File descalificadosFile = new File("config/paexium/descalificados.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("descalificar")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("parcela", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                int parcelaId = IntegerArgumentType.getInteger(ctx, "parcela");
                                ServerCommandSource source = ctx.getSource();
                                MinecraftServer server = source.getServer();

                                List<UUID> descalificados = new ArrayList<>();

                                Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();
                                if (asignaciones == null || asignaciones.isEmpty()) {
                                    source.sendError(Text.literal("❌ No hay asignaciones cargadas."));
                                    return 0;
                                }

                                for (Map.Entry<UUID, Integer> entry : asignaciones.entrySet()) {
                                    if (entry.getValue() == parcelaId) {
                                        UUID playerUUID = entry.getKey();
                                        descalificados.add(playerUUID);
                                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);
                                        if (player != null) {
                                            player.changeGameMode(GameMode.SPECTATOR);
                                            player.sendMessage(Text.literal("Has sido descalificado.").formatted(Formatting.RED), false);
                                        }
                                        server.getPlayerManager().broadcast(
                                                Text.literal("❌ Descalificado: " + playerName(server, playerUUID) + " (Parcela " + parcelaId + ")")
                                                        .formatted(Formatting.RED),
                                                false
                                        );
                                    }
                                }

                                if (!descalificados.isEmpty()) {
                                    guardarDescalificados(parcelaId, descalificados);
                                    source.sendFeedback(() -> Text.literal("✔ Jugadores descalificados exitosamente."), false);
                                } else {
                                    source.sendError(Text.literal("❌ No hay jugadores asignados a la parcela " + parcelaId + "."));
                                }

                                return 1;
                            })
                    )
            );
        });
    }

    private static String playerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        return server.getUserCache().getByUuid(uuid)
                .map(profile -> profile.getName())
                .orElse(uuid.toString());
    }

    private static void guardarDescalificados(int parcela, List<UUID> descalificados) {
        Map<String, List<String>> data = new HashMap<>();
        if (descalificadosFile.exists()) {
            try (FileReader reader = new FileReader(descalificadosFile)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<String> lista = new ArrayList<>();
        for (UUID uuid : descalificados) {
            lista.add(uuid.toString());
        }
        data.put(String.valueOf(parcela), lista);
        try (FileWriter writer = new FileWriter(descalificadosFile)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}