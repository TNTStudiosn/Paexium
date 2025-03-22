package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.votacion.VotacionManager;
import com.TNTStudios.paexium.votacion.VotacionManager.InfoParcela;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
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

                                RondaManager.RondaData rondaData = RondaManager.obtenerRondas().get(rondaNum);
                                if (rondaData == null) {
                                    source.sendError(Text.literal("❌ La ronda " + rondaNum + " no está configurada."));
                                    return 0;
                                }

                                int eliminadosConfig = rondaData.eliminados;
                                Map<String, InfoParcela> votingData = VotacionManager.getVotingDataForRound(String.valueOf(rondaNum));

                                if (votingData == null || votingData.isEmpty()) {
                                    source.sendError(Text.literal("❌ No hay datos de votación para la ronda " + rondaNum + "."));
                                    return 0;
                                }

                                List<Map.Entry<String, InfoParcela>> sorted = new ArrayList<>(votingData.entrySet());
                                sorted.sort(Comparator.comparingInt(e -> e.getValue().total));

                                LinkedHashMap<Integer, List<Map.Entry<String, InfoParcela>>> grouped = new LinkedHashMap<>();
                                for (Map.Entry<String, InfoParcela> entry : sorted) {
                                    int total = entry.getValue().total;
                                    grouped.computeIfAbsent(total, k -> new ArrayList<>()).add(entry);
                                }

                                List<Map.Entry<String, InfoParcela>> eliminables = new ArrayList<>();
                                List<Map.Entry<String, InfoParcela>> empate = new ArrayList<>();
                                int eliminadosPendientes = eliminadosConfig;

                                for (Map.Entry<Integer, List<Map.Entry<String, InfoParcela>>> groupEntry : grouped.entrySet()) {
                                    List<Map.Entry<String, InfoParcela>> grupo = groupEntry.getValue();
                                    int faltan = eliminadosConfig - eliminables.size();

                                    if (grupo.size() < faltan) {
                                        eliminables.addAll(grupo);
                                    } else if (grupo.size() == faltan) {
                                        eliminables.addAll(grupo);
                                        empate.clear();
                                        break;
                                    } else {
                                        empate.addAll(grupo);
                                        break;
                                    }
                                }

                                List<UUID> descalificados = new ArrayList<>();

                                // Eliminar los que claramente pierden
                                for (Map.Entry<String, InfoParcela> entry : eliminables) {
                                    int parcelaId = Integer.parseInt(entry.getKey());
                                    entry.getValue().votada = true;
                                    entry.getValue().esDesempate = false; // ✅ Ya no es una parcela en desempate
                                    eliminatePlayersFromParcel(server, parcelaId, descalificados, source);
                                }

                                // Si hay empate, marcar para desempate
                                if (!empate.isEmpty()) {
                                    for (Map.Entry<String, InfoParcela> entry : empate) {
                                        InfoParcela info = entry.getValue();
                                        info.votada = false;
                                        info.total = 0;
                                        info.esDesempate = true;
                                    }

                                    int faltantes = eliminadosConfig - eliminables.size();
                                    sendTitleSubtitleToAll(server, "Empate detectado", "Se hará desempate para eliminar " + faltantes + " parcela(s)", Formatting.YELLOW, Formatting.AQUA);

                                    source.sendFeedback(() -> Text.literal("✔ Se eliminaron " + eliminables.size() + " parcela(s). Empate para " + faltantes + " restante(s)."), false);
                                } else {
                                    sendTitleSubtitleToAll(server, "Jugadores descalificados", "La ronda ha finalizado", Formatting.RED, Formatting.GRAY);

                                    source.sendFeedback(() -> Text.literal("✔ Ronda " + rondaNum + " finalizada. Eliminados: " + descalificados.size()), false);
                                }

                                VotacionManager.setVotingDataForRound(String.valueOf(rondaNum), votingData);

                                if (!descalificados.isEmpty()) {
                                    guardarDescalificados(rondaNum, descalificados);
                                }

                                return 1;
                            })
                    )
            );
        });
    }

    private static void eliminatePlayersFromParcel(MinecraftServer server, int parcelId, List<UUID> descalificados, ServerCommandSource source) {
        Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();
        if (asignaciones == null) return;

        for (Map.Entry<UUID, Integer> entry : asignaciones.entrySet()) {
            if (entry.getValue() == parcelId) {
                UUID uuid = entry.getKey();
                descalificados.add(uuid);

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    player.changeGameMode(GameMode.SPECTATOR);
                }

                source.getServer().getPlayerManager().broadcast(
                        Text.literal("❌ Eliminado: " + playerName(server, uuid) + " (Parcela " + parcelId + ")")
                                .formatted(Formatting.RED), false
                );
            }
        }
    }

    private static void sendTitleSubtitleToAll(MinecraftServer server, String title, String subtitle, Formatting titleFormat, Formatting subtitleFormat) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title).formatted(titleFormat)));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(subtitleFormat)));
        }
    }

    private static String playerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) return player.getName().getString();

        return server.getUserCache().getByUuid(uuid)
                .map(profile -> profile.getName())
                .orElse("Desconocido (" + uuid + ")");
    }

    private static void guardarDescalificados(int ronda, List<UUID> descalificados) {
        Map<String, List<String>> data = new HashMap<>();
        if (descalificadosFile.exists()) {
            try (FileReader reader = new FileReader(descalificadosFile)) {
                Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
                Map<String, List<String>> loaded = gson.fromJson(reader, type);
                if (loaded != null) data = loaded;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<String> lista = new ArrayList<>();
        for (UUID uuid : descalificados) lista.add(uuid.toString());
        data.put(String.valueOf(ronda), lista);

        try (FileWriter writer = new FileWriter(descalificadosFile)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
