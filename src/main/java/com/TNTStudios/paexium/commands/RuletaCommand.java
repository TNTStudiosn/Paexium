package com.TNTStudios.paexium.commands;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;


import com.TNTStudios.paexium.network.RuletaNetworking; // <-- donde manejaremos el envío de packet

public class RuletaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ruleta")
                    .requires(source -> source.hasPermissionLevel(2)) // Solo OP (nivel 2+)
                    .executes(context -> {
                        ServerCommandSource scs = context.getSource();
                        ServerPlayerEntity player = scs.getPlayer();

                        // Escoger al azar una de las 8 opciones
                        int opcionGanadora = player.getRandom().nextInt(8);

                        // Enviar packet al cliente de ESTE jugador
                        RuletaNetworking.sendRuletaPacket(player, opcionGanadora);

                        scs.sendFeedback(() -> Text.literal("Girando la ruleta..."), false);
                        return Command.SINGLE_SUCCESS;
                    })
            );
        });
    }
}
