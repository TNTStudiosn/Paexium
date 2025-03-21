package com.TNTStudios.paexium.commands;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import com.TNTStudios.paexium.network.RuletaNetworking;
import java.util.ArrayList;
import java.util.List;

public class RuletaCommand {

    private static final List<Integer> availableOptions = new ArrayList<>();

    static {
        resetOptions();
    }

    private static void resetOptions() {
        availableOptions.clear();
        for (int i = 0; i < 8; i++) {
            availableOptions.add(i);
        }
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("ruleta")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        ServerCommandSource scs = context.getSource();
                        ServerPlayerEntity opPlayer = scs.getPlayer();

                        if (availableOptions.isEmpty()) {
                            resetOptions();
                            scs.sendFeedback(() -> Text.literal("Reiniciando ruleta"), false);
                        }

                        net.minecraft.util.math.random.Random random = opPlayer.getRandom();
                        int index = random.nextInt(availableOptions.size());
                        int opcionGanadora = availableOptions.remove(index);

                        long startServerTick = opPlayer.getWorld().getTime();

                        for (ServerPlayerEntity player : scs.getServer().getPlayerManager().getPlayerList()) {
                            RuletaNetworking.sendRuletaPacket(player, opcionGanadora, startServerTick);
                        }

                        scs.sendFeedback(() -> Text.literal("Girando la ruleta para todos los jugadores..."), false);
                        return Command.SINGLE_SUCCESS;
                    })
            );
        });
    }
}
