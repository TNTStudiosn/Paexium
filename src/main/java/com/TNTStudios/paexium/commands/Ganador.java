package com.TNTStudios.paexium.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;

public class Ganador {

    public static final Identifier MEDALLA_PACKET = new Identifier("paexium", "medalla");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("Ganador")
                    .then(CommandManager.argument("jugador", EntityArgumentType.player())
                            .then(CommandManager.argument("mensaje", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        ServerPlayerEntity ganador = EntityArgumentType.getPlayer(context, "jugador");
                                        String mensaje = StringArgumentType.getString(context, "mensaje");
                                        ejecutarEfectos(context.getSource(), ganador, mensaje);
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static void ejecutarEfectos(ServerCommandSource source, ServerPlayerEntity ganador, String mensaje) {
        try {
            // Efecto de fuegos artificiales en el ganador
            source.getServer().getCommandManager().getDispatcher().execute(
                    String.format("execute at %s run summon firework_rocket ~ ~ ~ {LifeTime:13,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Trail:1,Colors:[I;2437522,2651799,6719955],FadeColors:[I;2437522,14188952,6719955]}],Flight:1}}}}",
                            ganador.getEntityName()),
                    source
            );
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        // Enviar mensaje y sonido a todos los jugadores
        source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0F, 1.0F);

            // Enviar paquete para mostrar la medalla al cliente
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(ganador.getEntityName());
            buf.writeString(mensaje);
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket(MEDALLA_PACKET, buf));
        });
    }
}