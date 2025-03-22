package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.RondaManager;
import com.TNTStudios.paexium.parcelas.RondaManager.RondaData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Map;

public class CrearRondaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("ronda")
                    .requires(source -> source.hasPermissionLevel(4));

            // Subcomando: crear o editar
            root.then(CommandManager.literal("configurar")
                    .then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 4))
                            .then(CommandManager.argument("participantes", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("eliminados", IntegerArgumentType.integer(0))
                                            .then(CommandManager.argument("por_parcela", IntegerArgumentType.integer(1))
                                                    .then(CommandManager.argument("duracion", IntegerArgumentType.integer(1))
                                                            .executes(ctx -> {
                                                                int numero = IntegerArgumentType.getInteger(ctx, "numero");
                                                                int participantes = IntegerArgumentType.getInteger(ctx, "participantes");
                                                                int eliminados = IntegerArgumentType.getInteger(ctx, "eliminados");
                                                                int porParcela = IntegerArgumentType.getInteger(ctx, "por_parcela");
                                                                int duracion = IntegerArgumentType.getInteger(ctx, "duracion");

                                                                RondaManager.guardarRonda(numero, participantes, eliminados, porParcela, duracion);

                                                                ctx.getSource().sendFeedback(() ->
                                                                        Text.literal("✅ Ronda " + numero + " configurada correctamente."), false);
                                                                return 1;
                                                            })
                                                    )
                                            )
                                    )
                            )
                    )
            );


            // Subcomando: eliminar
            root.then(CommandManager.literal("eliminar")
                    .then(CommandManager.argument("numero", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> {
                                int numero = IntegerArgumentType.getInteger(ctx, "numero");

                                if (RondaManager.eliminarRonda(numero)) {
                                    ctx.getSource().sendFeedback(() ->
                                            Text.literal("🗑️ Ronda " + numero + " eliminada."), false);
                                } else {
                                    ctx.getSource().sendError(Text.literal("❌ La ronda " + numero + " no existe."));
                                }

                                return 1;
                            })));

            // Subcomando: listar
            root.then(CommandManager.literal("listar")
                    .executes(ctx -> {
                        Map<Integer, RondaData> rondas = RondaManager.obtenerRondas();
                        if (rondas.isEmpty()) {
                            ctx.getSource().sendFeedback(() -> Text.literal("📭 No hay rondas registradas."), false);
                            return 0;
                        }

                        ctx.getSource().sendFeedback(() -> Text.literal("📋 Rondas registradas:"), false);
                        for (Map.Entry<Integer, RondaData> entry : rondas.entrySet()) {
                            RondaData data = entry.getValue();
                            String info = String.format("▪ Ronda %d: Participantes=%d, Eliminados=%d, PorParcela=%d, Duración=%ds",
                                    entry.getKey(), data.participantes, data.eliminados, data.porParcela, data.duracion);
                            ctx.getSource().sendFeedback(() -> Text.literal(info), false);
                        }
                        return 1;
                    }));

            dispatcher.register(root);
        });
    }
}
