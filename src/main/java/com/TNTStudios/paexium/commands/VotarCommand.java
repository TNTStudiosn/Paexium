package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.items.PaexiumItems;
import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.votacion.VotacionManager;
import com.TNTStudios.paexium.votacion.VotacionManager.InfoParcela;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class VotarCommand {

    private static Integer currentParcelVoting = null;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("votar")
                    .then(CommandManager.argument("ronda", IntegerArgumentType.integer(1, 4))
                            .requires(source -> source.hasPermissionLevel(4))
                            .executes(context -> {
                                int rondaNum = IntegerArgumentType.getInteger(context, "ronda");
                                ServerCommandSource source = context.getSource();

                                RondaManager.RondaData rondaData = RondaManager.obtenerRondas().get(rondaNum);
                                if (rondaData == null) {
                                    source.sendError(Text.literal("❌ La ronda " + rondaNum + " no está configurada."));
                                    return 0;
                                }
                                int cantidadParcelasActivas = rondaData.participantes;

                                Map<Integer, Vec3i[]> parcelas = ParcelManager.getParcelas();
                                if (parcelas.isEmpty()) {
                                    source.sendError(Text.literal("⚠️ No hay parcelas registradas."));
                                    return 0;
                                }
                                List<Integer> parcelIds = new ArrayList<>(parcelas.keySet());
                                Collections.sort(parcelIds);
                                if (parcelIds.size() > cantidadParcelasActivas) {
                                    parcelIds = parcelIds.subList(0, cantidadParcelasActivas);
                                }

                                String roundKey = String.valueOf(rondaNum);
                                Map<String, InfoParcela> votingData = VotacionManager.getVotingDataForRound(roundKey);
                                if (votingData == null) {
                                    votingData = new HashMap<>();
                                    for (Integer pid : parcelIds) {
                                        votingData.put(String.valueOf(pid), new InfoParcela());
                                    }
                                    VotacionManager.setVotingDataForRound(roundKey, votingData);
                                } else {
                                    for (Integer pid : parcelIds) {
                                        String pidStr = String.valueOf(pid);
                                        if (!votingData.containsKey(pidStr)) {
                                            votingData.put(pidStr, new InfoParcela());
                                        }
                                    }
                                }

                                boolean hayDesempate = votingData.values().stream().anyMatch(info -> info.esDesempate);
                                if (hayDesempate) {
                                    Iterator<Integer> iterator = parcelIds.iterator();
                                    while (iterator.hasNext()) {
                                        int id = iterator.next();
                                        InfoParcela info = votingData.get(String.valueOf(id));
                                        if (info == null || !info.esDesempate) {
                                            iterator.remove();
                                        }
                                    }
                                }

                                // ✅ Marcar como votada la anterior ANTES de buscar la siguiente
                                if (currentParcelVoting != null) {
                                    InfoParcela currentInfo = votingData.get(String.valueOf(currentParcelVoting));
                                    if (currentInfo != null) currentInfo.votada = true;
                                }

                                // 🔍 Buscar siguiente no votada
                                Integer nextParcel = null;
                                for (Integer pid : parcelIds) {
                                    InfoParcela info = votingData.get(String.valueOf(pid));
                                    if (!info.votada) {
                                        nextParcel = pid;
                                        break;
                                    }
                                }

                                if (nextParcel == null) {
                                    currentParcelVoting = null; // Limpiar
                                    source.sendFeedback(() -> Text.literal("*Todas las parcelas fueron votadas*").formatted(Formatting.GOLD), false);
                                    return 1;
                                }

                                // 🧠 Actualizar actual en votación
                                currentParcelVoting = nextParcel;

                                // 🧭 Teletransporte
                                Vec3i[] coords = parcelas.get(nextParcel);
                                Vec3i min = coords[0];
                                double centerX = min.getX() + 0.5;
                                double centerY = min.getY() + 1;
                                double centerZ = min.getZ() + 0.5;

                                Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();

                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    player.teleport(player.getServerWorld(), centerX, centerY, centerZ, player.getYaw(), player.getPitch());
                                    if (esJuez(player.getUuid()) || (asignaciones != null && asignaciones.getOrDefault(player.getUuid(), -1) == nextParcel)) {
                                        player.changeGameMode(GameMode.CREATIVE);
                                    } else {
                                        player.changeGameMode(GameMode.SPECTATOR);
                                    }
                                }

                                InfoParcela actual = votingData.get(String.valueOf(nextParcel));
                                final Integer finalNextParcel = nextParcel;
                                source.sendFeedback(() -> Text.literal("📍 Votación iniciada para la parcela " + finalNextParcel), false);


                                if (actual != null && actual.esDesempate) {
                                    source.sendFeedback(() -> Text.literal("🟡 Esta es una votacion de desempate.").formatted(Formatting.YELLOW), false);
                                    for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                        player.sendMessage(Text.literal("🟡 Iniciando votación de desempate").formatted(Formatting.YELLOW), false);
                                    }
                                }

                                VotacionManager.setVotingDataForRound(roundKey, votingData);

                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    if (esJuez(player.getUuid())) {
                                        player.getInventory().clear();
                                        player.getInventory().insertStack(PaexiumItems.PALETA_1.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_2.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_3.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_4.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_5.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_6.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_7.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_8.getDefaultStack());
                                        player.getInventory().insertStack(PaexiumItems.PALETA_9.getDefaultStack());
                                        player.sendMessage(Text.literal("📦 Se te han entregado las paletas de votación").formatted(Formatting.GREEN), false);
                                    }
                                }

                                return 1;
                            })));
        });
    }

    private static boolean esJuez(UUID uuid) {
        try {
            return net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(uuid)
                    .getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}
