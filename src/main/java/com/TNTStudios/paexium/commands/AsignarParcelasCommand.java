package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.parcelas.RondaManager.RondaData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class AsignarParcelasCommand {

    private static final File file = new File("config/paexium/asignaciones.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(CommandManager.literal("asignarparcelas")
                    .requires(src -> src.hasPermissionLevel(4))
                    .then(CommandManager.argument("ronda", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerCommandSource source = ctx.getSource();
                                int rondaNum = IntegerArgumentType.getInteger(ctx, "ronda");
                                RondaData ronda = RondaManager.obtenerRondas().get(rondaNum);

                                if (ronda == null) {
                                    source.sendError(Text.literal("❌ La ronda " + rondaNum + " no está configurada."));
                                    return 0;
                                }

                                int maxPorParcela = ronda.porParcela;
                                List<ServerPlayerEntity> jugadoresValidos = new ArrayList<>();
                                for (ServerPlayerEntity jugador : source.getServer().getPlayerManager().getPlayerList()) {
                                    if (jugador.interactionManager.getGameMode() == GameMode.SPECTATOR) continue;
                                    if (esJuez(jugador.getUuid())) continue;
                                    jugadoresValidos.add(jugador);
                                }

                                List<Integer> parcelasDisponibles = new ArrayList<>(ParcelManager.getParcelas().keySet());
                                Collections.sort(parcelasDisponibles);

                                Map<UUID, Integer> asignaciones = new HashMap<>();
                                int jugadorIndex = 0;

                                for (int parcelaId : parcelasDisponibles) {
                                    for (int i = 0; i < maxPorParcela && jugadorIndex < jugadoresValidos.size(); i++) {
                                        ServerPlayerEntity jugador = jugadoresValidos.get(jugadorIndex++);
                                        asignaciones.put(jugador.getUuid(), parcelaId);

                                        source.sendFeedback(() ->
                                                Text.literal("✅ Jugador " + jugador.getName().getString() +
                                                        " asignado a parcela " + parcelaId), false);

                                        jugador.networkHandler.sendPacket(new TitleS2CPacket(
                                                Text.literal("📍 Parcela " + parcelaId).formatted(Formatting.GOLD)
                                        ));
                                        jugador.networkHandler.sendPacket(new SubtitleS2CPacket(
                                                Text.literal("¡Has sido asignado a tu parcela!").formatted(Formatting.AQUA)
                                        ));
                                    }
                                    if (jugadorIndex >= jugadoresValidos.size()) break;
                                }

                                guardarAsignaciones(asignaciones);
                                return 1;
                            })));
        });
    }

    private static boolean esJuez(UUID uuid) {
        try {
            UserManager um = LuckPermsProvider.get().getUserManager();
            User user = um.getUser(uuid);
            return user != null && user.getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static void guardarAsignaciones(Map<UUID, Integer> asignaciones) {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(asignaciones, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<UUID, Integer> cargarAsignaciones() {
        if (!file.exists()) return null;
        try (FileReader reader = new FileReader(file)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<UUID, Integer>>(){}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
