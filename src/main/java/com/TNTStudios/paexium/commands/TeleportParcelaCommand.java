package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3i;

import java.util.Map;


public class TeleportParcelaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("tparcela")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("id", IntegerArgumentType.integer())
                            .suggests(PARCELA_SUGGESTIONS)
                            .executes(TeleportParcelaCommand::run)
                    );

            dispatcher.register(command);
        });
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        int id = IntegerArgumentType.getInteger(context, "id");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Map<Integer, Vec3i[]> parcelas = ParcelManager.getParcelas();

        if (!parcelas.containsKey(id)) {
            source.sendError(Text.literal("❌ La parcela " + id + " no existe."));
            return 0;
        }

        Vec3i[] coords = parcelas.get(id);
        Vec3i target = coords[0]; // esquina pos1

        // 📦 Teleport +1 en altura
        player.teleport(player.getServerWorld(), target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5, player.getYaw(), player.getPitch());
        source.sendFeedback(() -> Text.literal("📍 Teletransportado a la parcela " + id), false);
        return Command.SINGLE_SUCCESS;
    }


    // Autocompletado dinámico
    private static final SuggestionProvider<ServerCommandSource> PARCELA_SUGGESTIONS = (context, builder) -> {
        ParcelManager.getParcelas().keySet().stream()
                .map(String::valueOf)
                .forEach(builder::suggest);
        return builder.buildFuture();
    };
}
