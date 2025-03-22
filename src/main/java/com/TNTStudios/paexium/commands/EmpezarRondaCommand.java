package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.parcelas.RondaManager.RondaData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

public class EmpezarRondaCommand {

    private static final File asignacionesFile = new File("config/paexium/asignaciones.json");
    private static final Gson gson = new Gson();
    private static boolean isRunning = false;
    private static int countdown = 0;
    private static int tickCounter = 0;
    private static ServerBossBar bossBar;
    private static MinecraftServer serverInstance;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("empezarronda")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> {
                                int numero = IntegerArgumentType.getInteger(ctx, "numero");
                                ServerCommandSource source = ctx.getSource();
                                MinecraftServer server = source.getServer();
                                serverInstance = server;

                                RondaData ronda = RondaManager.obtenerRondas().get(numero);
                                if (ronda == null) {
                                    source.sendError(Text.literal("❌ La ronda " + numero + " no está configurada."));
                                    return 0;
                                }

                                Map<UUID, Integer> asignaciones = cargarAsignaciones();
                                if (asignaciones == null || asignaciones.isEmpty()) {
                                    source.sendError(Text.literal("❌ No hay asignaciones disponibles."));
                                    return 0;
                                }

                                Map<Integer, Vec3i[]> parcelas = ParcelManager.getParcelas();
                                Map<Integer, List<ServerPlayerEntity>> grupo = new HashMap<>();

                                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                    if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) continue;
                                    if (esJuez(player.getUuid())) continue;

                                    Integer parcelaId = asignaciones.get(player.getUuid());
                                    if (parcelaId == null || !parcelas.containsKey(parcelaId)) continue;

                                    grupo.computeIfAbsent(parcelaId, k -> new ArrayList<>()).add(player);
                                }

                                for (Map.Entry<Integer, List<ServerPlayerEntity>> entry : grupo.entrySet()) {
                                    Vec3i[] pos = parcelas.get(entry.getKey());
                                    Vec3i min = pos[0];
                                    Vec3i max = pos[1];
                                    double x = (min.getX() + max.getX()) / 2.0 + 0.5;
                                    double y = (min.getY() + max.getY()) / 2.0 + 1;
                                    double z = (min.getZ() + max.getZ()) / 2.0 + 0.5;

                                    for (ServerPlayerEntity p : entry.getValue()) {
                                        p.teleport(p.getServerWorld(), x, y, z, p.getYaw(), p.getPitch());
                                    }
                                }

                                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                    if (esJuez(player.getUuid())) {
                                        player.changeGameMode(GameMode.SPECTATOR);
                                    }
                                }

                                iniciarTemporizador(server, ronda.duracion);
                                return 1;
                            })
                    )
            );
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (!isRunning) return;

            tickCounter++;
            if (tickCounter >= 20) {
                countdown--;
                tickCounter = 0;
                actualizarBossbar();

                if (countdown <= 0) {
                    finalizarTemporizador();
                }
            }
        });
    }

    private static Map<UUID, Integer> cargarAsignaciones() {
        if (!asignacionesFile.exists()) return null;
        try (FileReader reader = new FileReader(asignacionesFile)) {
            Type type = new TypeToken<Map<UUID, Integer>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean esJuez(UUID uuid) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            return user != null && user.getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static void iniciarTemporizador(MinecraftServer server, int duracion) {
        countdown = duracion;
        isRunning = true;
        tickCounter = 0;

        ensureBossBarInitialized();
        bossBar.setVisible(true);
        bossBar.clearPlayers();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            bossBar.addPlayer(player);
        }

        actualizarBossbar();
    }

    private static void ensureBossBarInitialized() {
        if (bossBar == null) {
            bossBar = new ServerBossBar(Text.literal("⏳ Tiempo restante"), BossBar.Color.WHITE, BossBar.Style.PROGRESS);
        }
    }


    private static void actualizarBossbar() {
        if (bossBar == null) return;
        String timeText = String.format("%d:%02d", countdown / 60, countdown % 60);
        bossBar.setName(Text.literal("⏳ Tiempo restante: " + timeText).formatted(Formatting.WHITE));
        bossBar.setPercent(Math.max(0f, countdown / 300f));
    }

    private static void finalizarTemporizador() {
        isRunning = false;
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.clearPlayers();
        }

        for (ServerPlayerEntity player : serverInstance.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("⏰ Tiempo terminado").formatted(Formatting.RED)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("¡La ronda ha finalizado!").formatted(Formatting.GRAY)
            ));
        }
    }
}
