package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.votacion.VotacionManager;
import com.TNTStudios.paexium.votacion.VotacionManager.InfoParcela;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
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

public class ResultadosCommand {

    // Archivo para guardar los jugadores descalificados por ronda.
    private static final File descalificadosFile = new File("config/paexium/descalificados.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("resultados")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("ronda", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> {
                                int rondaNum = IntegerArgumentType.getInteger(ctx, "ronda");
                                ServerCommandSource source = ctx.getSource();
                                MinecraftServer server = source.getServer();

                                // Verificar que la ronda esté configurada
                                RondaManager.RondaData rondaData = RondaManager.obtenerRondas().get(rondaNum);
                                if (rondaData == null) {
                                    source.sendError(Text.literal("❌ La ronda " + rondaNum + " no está configurada."));
                                    return 0;
                                }
                                int eliminadosConfig = rondaData.eliminados;

                                // Leer los datos de votación para la ronda
                                Map<String, InfoParcela> votingData = VotacionManager.getVotingDataForRound(String.valueOf(rondaNum));
                                if (votingData == null || votingData.isEmpty()) {
                                    source.sendError(Text.literal("❌ No hay datos de votación para la ronda " + rondaNum + "."));
                                    return 0;
                                }

                                // Ordenar las parcelas según el total de votos (de menor a mayor)
                                List<Map.Entry<String, InfoParcela>> sortedParcels = new ArrayList<>(votingData.entrySet());
                                sortedParcels.sort(Comparator.comparingInt(entry -> entry.getValue().total));

                                // Agrupar por puntaje: clave = total de votos, valor = lista de entradas (parcelas) con ese puntaje
                                LinkedHashMap<Integer, List<Map.Entry<String, InfoParcela>>> grouped = new LinkedHashMap<>();
                                for (Map.Entry<String, InfoParcela> entry : sortedParcels) {
                                    int total = entry.getValue().total;
                                    grouped.computeIfAbsent(total, k -> new ArrayList<>()).add(entry);
                                }

                                int eliminatedCount = 0;
                                List<UUID> descalificados = new ArrayList<>();
                                boolean tieBreakNeeded = false;
                                int parcelsToEliminateInTie = 0;

                                // Procesar los grupos en orden ascendente de votos
                                for (Map.Entry<Integer, List<Map.Entry<String, InfoParcela>>> groupEntry : grouped.entrySet()) {
                                    List<Map.Entry<String, InfoParcela>> groupList = groupEntry.getValue();
                                    int groupSize = groupList.size();

                                    if (eliminatedCount + groupSize < eliminadosConfig) {
                                        // Se puede eliminar el grupo completo sin exceder la configuración
                                        for (Map.Entry<String, InfoParcela> parcelEntry : groupList) {
                                            String parcelKey = parcelEntry.getKey();
                                            int parcelId = Integer.parseInt(parcelKey);
                                            // Marcar la parcela como votada (eliminada)
                                            parcelEntry.getValue().votada = true;
                                            // Eliminar a los jugadores asignados a esta parcela
                                            eliminatePlayersFromParcel(server, parcelId, descalificados, source);
                                        }
                                        eliminatedCount += groupSize;
                                    } else if (eliminatedCount + groupSize == eliminadosConfig) {
                                        // Eliminando el grupo se alcanza exactamente el número requerido
                                        for (Map.Entry<String, InfoParcela> parcelEntry : groupList) {
                                            String parcelKey = parcelEntry.getKey();
                                            int parcelId = Integer.parseInt(parcelKey);
                                            parcelEntry.getValue().votada = true;
                                            eliminatePlayersFromParcel(server, parcelId, descalificados, source);
                                        }
                                        eliminatedCount += groupSize;
                                        // Enviar title y subtitle de jugadores descalificados
                                        sendTitleSubtitleToAll(server, "Jugadores descalificados", "La ronda ha finalizado", Formatting.RED, Formatting.GRAY);
                                        break; // Se cumple el total de eliminaciones
                                    } else {
                                        // Eliminar el grupo completo excedería el número de eliminaciones requeridas: hay empate
                                        tieBreakNeeded = true;
                                        parcelsToEliminateInTie = eliminadosConfig - eliminatedCount;
                                        // Dejar la bandera en false para que puedan re-votarse
                                        for (Map.Entry<String, InfoParcela> parcelEntry : groupList) {
                                            parcelEntry.getValue().votada = false;
                                        }
                                        // Enviar title y subtitle indicando empate y nueva votación
                                        sendTitleSubtitleToAll(server, "Empate detectado", "Se realizará desempate para eliminar " + parcelsToEliminateInTie + " parcela(s)", Formatting.YELLOW, Formatting.AQUA);
                                        break; // No se procesan más grupos
                                    }
                                }

                                // Guardar la data de votación actualizada (con las parcelas empatadas marcadas para desempate)
                                VotacionManager.setVotingDataForRound(String.valueOf(rondaNum), votingData);

                                // Guardar en el archivo JSON la lista de jugadores descalificados (los que fueron eliminados en este paso)
                                if (!descalificados.isEmpty()) {
                                    guardarDescalificados(rondaNum, descalificados);
                                }

                                // Feedback al admin
                                if (tieBreakNeeded) {
                                    final int finalEliminatedCount = eliminatedCount;
                                    final int finalParcelsToEliminateInTie = parcelsToEliminateInTie;
                                    source.sendFeedback(
                                            () -> Text.literal("✔ Se han eliminado " + finalEliminatedCount + " parcela(s). " +
                                                    "Empate en el grupo para eliminar " + finalParcelsToEliminateInTie + " parcela(s)."),
                                            false
                                    );
                                } else {
                                    source.sendFeedback(
                                            () -> Text.literal("✔ Resultados de la ronda " + rondaNum + " procesados. " +
                                                    descalificados.size() + " jugador(es) eliminados."),
                                            false
                                    );
                                }

                                return 1;
                            })
                    )
            );
        });
    }

    /**
     * Método auxiliar para eliminar a los jugadores asignados a una parcela.
     * Se consulta el JSON de asignaciones y, para cada jugador asignado a la parcela,
     * se cambia su modo de juego a SPECTATOR y se anuncia su eliminación.
     */
    private static void eliminatePlayersFromParcel(MinecraftServer server, int parcelId, List<UUID> descalificados, ServerCommandSource source) {
        Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();
        if (asignaciones == null || asignaciones.isEmpty()) return;
        for (Map.Entry<UUID, Integer> entry : asignaciones.entrySet()) {
            if (entry.getValue() == parcelId) {
                UUID playerUUID = entry.getKey();
                descalificados.add(playerUUID);
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);
                if (player != null) {
                    player.changeGameMode(GameMode.SPECTATOR);
                }
                source.getServer().getPlayerManager().broadcast(
                        Text.literal("❌ Eliminado: " + playerName(server, playerUUID) + " (Parcela " + parcelId + ")")
                                .formatted(Formatting.RED),
                        false
                );
            }
        }
    }

    /**
     * Método auxiliar para enviar un Title y Subtitle a todos los jugadores conectados.
     */
    private static void sendTitleSubtitleToAll(MinecraftServer server, String titleText, String subtitleText, Formatting titleFormat, Formatting subtitleFormat) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal(titleText).formatted(titleFormat)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal(subtitleText).formatted(subtitleFormat)
            ));
        }
    }

    /**
     * Método auxiliar para obtener el nombre del jugador a partir de su UUID.
     * Si el jugador está online se retorna su nombre; de lo contrario se consulta el caché.
     */
    private static String playerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        return server.getUserCache().getByUuid(uuid)
                .map(profile -> profile.getName())
                .orElse(uuid.toString());
    }

    /**
     * Método auxiliar para guardar en un archivo JSON los jugadores descalificados en la ronda.
     */
    private static void guardarDescalificados(int ronda, List<UUID> descalificados) {
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
        data.put(String.valueOf(ronda), lista);
        try (FileWriter writer = new FileWriter(descalificadosFile)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
