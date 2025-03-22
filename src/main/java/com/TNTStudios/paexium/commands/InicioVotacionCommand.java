package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.items.PaexiumItems;
import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.parcelas.RondaManager.RondaData;
import com.TNTStudios.paexium.votacion.VotacionManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.UUID;

public class InicioVotacionCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("iniciovotacion")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("ronda", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> {
                                int numeroRonda = IntegerArgumentType.getInteger(ctx, "ronda");
                                ServerCommandSource source = ctx.getSource();

                                // Verificar si la ronda existe
                                RondaData data = RondaManager.obtenerRondas().get(numeroRonda);
                                if (data == null) {
                                    source.sendError(Text.literal("❌ La ronda " + numeroRonda + " no está configurada."));
                                    return 0;
                                }

                                // Mostrar título y subtítulo a todos
                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    player.networkHandler.sendPacket(new TitleS2CPacket(
                                            Text.literal("Inicio de votación de jueces").formatted(Formatting.YELLOW)
                                    ));
                                    player.networkHandler.sendPacket(new SubtitleS2CPacket(
                                            Text.literal("Ronda #" + numeroRonda).formatted(Formatting.GOLD)
                                    ));
                                }

                                // Poner a todos en modo espectador
                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    player.changeGameMode(GameMode.SPECTATOR);
                                }

                                // Poner a los jueces en modo creativo y dar paletas
                                for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                                    if (esJuez(player.getUuid())) {
                                        player.changeGameMode(GameMode.CREATIVE);


                                    }
                                }

                                // Inicializar la votación en VotacionManager
                                VotacionManager.iniciarVotacion(numeroRonda);

                                source.sendFeedback(() ->
                                        Text.literal("✔ Votación de la ronda " + numeroRonda + " iniciada."), false
                                );
                                return 1;
                            })
                    )
            );
        });
    }

    private static boolean esJuez(UUID uuid) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            return user != null && user.getCachedData().getPermissionData().checkPermission("paexium.juez").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}
