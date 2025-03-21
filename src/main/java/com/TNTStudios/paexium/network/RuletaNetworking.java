package com.TNTStudios.paexium.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import io.netty.buffer.Unpooled;

public class RuletaNetworking {

    // Canal para identificar el packet
    public static final Identifier RULETA_PACKET_ID = new Identifier("paexium", "ruleta_packet");

    // Llamado desde el comando para enviar al cliente
    public static void sendRuletaPacket(ServerPlayerEntity player, int opcion) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(opcion);

        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(RULETA_PACKET_ID, buf);
        player.networkHandler.sendPacket(packet);
    }
}
