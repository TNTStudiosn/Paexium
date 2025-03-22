package com.TNTStudios.paexium.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

public class LimpiarRondaCommand {

    // Archivo donde se guardan los jugadores descalificados
    private static final File descalificadosFile = new File("config/paexium/descalificados.json");
    // Archivo de asignaciones que se limpiará para la siguiente ronda
    private static final File asignacionesFile = new File("config/paexium/asignaciones.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("limpiarronda")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        MinecraftServer server = source.getServer();

                        // Cargar los jugadores descalificados de descalificados.json
                        Set<UUID> disqualified = new HashSet<>();
                        if (descalificadosFile.exists()) {
                            try (FileReader reader = new FileReader(descalificadosFile)) {
                                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                                Map<String, List<String>> data = gson.fromJson(reader, type);
                                if (data != null) {
                                    // Aquí se unen los jugadores descalificados de todas las rondas
                                    for (List<String> list : data.values()) {
                                        for (String s : list) {
                                            disqualified.add(UUID.fromString(s));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        int changedCount = 0;
                        // Iterar sobre todos los jugadores conectados:
                        // Los que NO estén en la lista de descalificados pasan a CREATIVE.
                        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                            if (!disqualified.contains(player.getUuid())) {
                                player.changeGameMode(GameMode.CREATIVE);
                                changedCount++;
                            }
                        }

                        // Limpiar el archivo de asignaciones: se elimina para que quede vacío.
                        if (asignacionesFile.exists()) {
                            if (!asignacionesFile.delete()) {
                                source.sendError(Text.literal("❌ No se pudo limpiar el archivo de asignaciones."));
                            }
                        }

                        final int finalChangedCount = changedCount;
                        source.sendFeedback(
                                () -> Text.literal("✔ Ronda finalizada. " + finalChangedCount + " jugador(es) puestos en CREATIVE. Asignaciones reiniciadas."),
                                false
                        );

                        return 1;
                    })
            );
        });
    }
}
