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

    // Variable para rastrear la parcela actualmente en votación
    private static Integer currentParcelVoting = null;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("votar")
                    .then(CommandManager.argument("ronda", IntegerArgumentType.integer(1, 4))
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        int rondaNum = IntegerArgumentType.getInteger(context, "ronda");
                        ServerCommandSource source = context.getSource();

                        RondaManager.RondaData rondaData = com.TNTStudios.paexium.parcelas.RondaManager.obtenerRondas().get(rondaNum);
                        if (rondaData == null) {
                            source.sendError(Text.literal("❌ La ronda " + rondaNum + " no está configurada."));
                            return 0;
                        }
                        int cantidadParcelasActivas = rondaData.participantes;


                        // Obtener todas las parcelas y ordenarlas de menor a mayor ID
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


                        // Obtener (o inicializar) la data de votación para la ronda actual
                        String roundKey = String.valueOf(rondaNum);
                        Map<String, InfoParcela> votingData = VotacionManager.getVotingDataForRound(roundKey);
                        if (votingData == null) {
                            votingData = new HashMap<>();
                            for (Integer pid : parcelIds) {
                                votingData.put(String.valueOf(pid), new InfoParcela());
                            }
                            VotacionManager.setVotingDataForRound(roundKey, votingData);
                        } else {
                            // Asegurar que estén todas las parcelas en la data
                            for (Integer pid : parcelIds) {
                                String pidStr = String.valueOf(pid);
                                if (!votingData.containsKey(pidStr)) {
                                    votingData.put(pidStr, new InfoParcela());
                                }
                            }
                        }

                        // Buscar la siguiente parcela que no haya sido votada
                        Integer nextParcel = null;
                        for (Integer pid : parcelIds) {
                            InfoParcela info = votingData.get(String.valueOf(pid));
                            if (!info.votada) {
                                nextParcel = pid;
                                break;
                            }
                        }

                        if (nextParcel == null) {
                            source.sendFeedback(() -> Text.literal("*Todas las parcelas fueron votadas*").formatted(Formatting.GOLD), false);
                            return 1;
                        }

                        // Si hay una parcela en votación activa, poner a sus jugadores en modo SPECTATOR y marcarla como votada
                        if (currentParcelVoting != null) {
                            InfoParcela currentInfo = votingData.get(String.valueOf(currentParcelVoting));
                            if (currentInfo != null) {
                                currentInfo.votada = true; // 🔁 Marcar como votada *antes* de buscar la siguiente
                            }

                            Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();
                            if (asignaciones != null) {
                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    if (asignaciones.getOrDefault(player.getUuid(), -1) == currentParcelVoting) {
                                        player.changeGameMode(GameMode.SPECTATOR);
                                    }
                                }
                            }
                        }


                        // Actualizar la parcela actual en votación
                        currentParcelVoting = nextParcel;

                        // Calcular el centro de la parcela seleccionada
                        Vec3i[] coords = parcelas.get(nextParcel);
                        Vec3i min = coords[0];
                        Vec3i max = coords[1];
                        double centerX = min.getX() + 0.5;
                        double centerY = min.getY() + 1;
                        double centerZ = min.getZ() + 0.5;

                        // Obtener asignaciones (si existen) para saber qué jugadores están asignados a cada parcela
                        Map<UUID, Integer> asignaciones = AsignarParcelasCommand.cargarAsignaciones();

                        // Teletransportar a todos los jugadores y asignarles el gamemode adecuado:
                        // Si el jugador es juez (permiso paexium.juez) o está asignado a la parcela actual, se pone en CREATIVE; en caso contrario, en SPECTATOR.
                        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                            player.teleport(player.getServerWorld(), centerX, centerY, centerZ, player.getYaw(), player.getPitch());
                            if (esJuez(player.getUuid()) || (asignaciones != null && asignaciones.getOrDefault(player.getUuid(), -1) == nextParcel)) {
                                player.changeGameMode(GameMode.CREATIVE);
                            } else {
                                player.changeGameMode(GameMode.SPECTATOR);
                            }
                        }

                        final Integer finalNextParcel = nextParcel;
                        source.sendFeedback(() -> Text.literal("📍 Votación iniciada para la parcela " + finalNextParcel), false);
                        // Actualiza la data de votación persistente
                        VotacionManager.setVotingDataForRound(roundKey, votingData);

                        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                            if (esJuez(player.getUuid())) {
                                player.getInventory().clear(); // Limpia inventario por si aún tienen paletas anteriores

                                // Entregar paletas de votación
                                player.getInventory().insertStack(PaexiumItems.PALETA_1.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_2.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_3.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_4.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_5.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_6.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_7.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_8.getDefaultStack());
                                player.getInventory().insertStack(PaexiumItems.PALETA_9.getDefaultStack());

                                // Mensaje de confirmación
                                player.sendMessage(Text.literal("📦 Se te han entregado las paletas de votación").formatted(Formatting.GREEN), false);
                            }
                        }

                        return 1;
                    })));
        });
    }

    // Método auxiliar para verificar si un jugador posee el permiso "paexium.juez"
    private static boolean esJuez(UUID uuid) {
        try {
            return net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(uuid)
                    .getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}
