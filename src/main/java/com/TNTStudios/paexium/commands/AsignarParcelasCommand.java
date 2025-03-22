package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        List<ServerPlayerEntity> jugadores = new ArrayList<>();
                        List<Integer> parcelasDisponibles = new ArrayList<>(ParcelManager.getParcelas().keySet());

                        // ✅ Ordenar parcelas para asignar desde la número 1
                        Collections.sort(parcelasDisponibles);

                        Map<UUID, Integer> asignaciones = new HashMap<>();
                        int index = 0;

                        for (ServerPlayerEntity jugador : source.getServer().getPlayerManager().getPlayerList()) {
                            if (index >= parcelasDisponibles.size()) break;

                            if (jugador.interactionManager.getGameMode() == GameMode.SPECTATOR) continue;
                            if (esJuez(jugador.getUuid())) continue;

                            int parcela = parcelasDisponibles.get(index++);
                            asignaciones.put(jugador.getUuid(), parcela);

                            // 📢 Mensaje al admin
                            source.sendFeedback(() ->
                                    Text.literal("✅ Jugador " + jugador.getName().getString() + " asignado a parcela " + parcela), false);

                            // 🪧 Título para el jugador
                            jugador.networkHandler.sendPacket(new TitleS2CPacket(
                                    Text.literal(jugador.getName().getString()).formatted(Formatting.GOLD)
                            ));
                            jugador.networkHandler.sendPacket(new SubtitleS2CPacket(
                                    Text.literal("Has sido asignado a la parcela " + parcela).formatted(Formatting.AQUA)
                            ));
                        }

                        guardarAsignaciones(asignaciones);
                        return 1;
                    }));
        });
    }

    private static boolean esJuez(UUID uuid) {
        try {
            UserManager userManager = LuckPermsProvider.get().getUserManager();
            User user = userManager.getUser(uuid);
            if (user == null) return false;

            return user.getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
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
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<UUID, Integer>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
