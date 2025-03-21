package com.TNTStudios.paexium.commands;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import com.TNTStudios.paexium.network.RuletaNetworking;

public class RuletaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ruleta")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        ServerCommandSource scs = context.getSource();
                        ServerPlayerEntity player = scs.getPlayer();

                        int opcionGanadora = player.getRandom().nextInt(8);

                        long startServerTick = player.getWorld().getTime();

                        RuletaNetworking.sendRuletaPacket(player, opcionGanadora, startServerTick);

                        scs.sendFeedback(() -> Text.literal("Girando la ruleta..."), false);
                        return Command.SINGLE_SUCCESS;
                    })
            );
        });
    }
}
