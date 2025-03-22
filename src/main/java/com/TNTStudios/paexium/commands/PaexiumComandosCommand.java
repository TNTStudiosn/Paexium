package com.TNTStudios.paexium.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PaexiumComandosCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("PaexiumComandos")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();

                        // Construir el mensaje con todos los comandos y sus breves explicaciones.
                        String message = ""
                                + "§6==== Comandos Paexium ====\n"
                                + "§e/asignarparcelas §7- Asigna parcelas a los jugadores según la ronda.\n"
                                + "§e/ronda configurar §7- Configura una nueva ronda (participantes, eliminados, etc.).\n"
                                + "§e/ronda eliminar §7- Elimina una ronda configurada.\n"
                                + "§e/ronda listar §7- Lista todas las rondas configuradas.\n"
                                + "§e/descalificar §7- Descalifica jugadores de una parcela.\n"
                                + "§e/empezarronda §7- Inicia la ronda y teletransporta a los jugadores.\n"
                                + "§e/Ganador §7- Otorga efectos especiales y medalla al ganador.\n"
                                + "§e/iniciovotacion §7- Inicia el proceso de votación para los jueces.\n"
                                + "§e/limpiarronda §7- Limpia la ronda y reinicia asignaciones.\n"
                                + "§e/CrearParcela §7- Crea una nueva parcela usando WorldEdit.\n"
                                + "§e/reset parcelas §7- Reinicia las parcelas (restablece el mundo).\n"
                                + "§e/resultados §7- Procesa y muestra los resultados de la votación.\n"
                                + "§e/ruleta §7- Ejecuta la ruleta para seleccionar una opción aleatoria.\n"
                                + "§e/tparcela §7- Teletransporta al jugador a una parcela específica.\n"
                                + "§e/verparcela §7- Muestra las asignaciones actuales de parcelas.\n"
                                + "§e/votar §7- Permite votar por una parcela durante la votación.\n"
                                + "§6==========================";

                        // Se envía el mensaje solo al jugador que ejecutó el comando
                        source.sendFeedback(() -> Text.literal(message), false);
                        return 1;
                    })
            );
        });
    }
}
