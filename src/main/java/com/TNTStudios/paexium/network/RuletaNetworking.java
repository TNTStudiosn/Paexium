package com.TNTStudios.paexium.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import io.netty.buffer.Unpooled;

public class RuletaNetworking {

    public static final Identifier RULETA_PACKET_ID = new Identifier("paexium", "ruleta_packet");

    public static void sendRuletaPacket(ServerPlayerEntity player, int opcion, long startTick) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(opcion);
        buf.writeLong(startTick);

        CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(RULETA_PACKET_ID, buf);
        player.networkHandler.sendPacket(packet);
    }
}
