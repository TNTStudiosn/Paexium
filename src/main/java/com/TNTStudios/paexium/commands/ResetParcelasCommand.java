package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Map;

public class ResetParcelasCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(CommandManager.literal("reset")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.literal("parcelas")
                            .executes(ctx -> {
                                ServerCommandSource source = ctx.getSource();
                                ServerWorld world = source.getWorld();

                                Map<Integer, Vec3i[]> parcelas = ParcelManager.getParcelas();
                                if (parcelas.isEmpty()) {
                                    source.sendError(Text.literal("⚠️ No hay parcelas registradas."));
                                    return 0;
                                }

                                int count = 0;
                                for (Map.Entry<Integer, Vec3i[]> entry : parcelas.entrySet()) {
                                    Vec3i p1 = entry.getValue()[0];
                                    Vec3i p2 = entry.getValue()[1];

                                    int minX = Math.min(p1.getX(), p2.getX());
                                    int maxX = Math.max(p1.getX(), p2.getX());
                                    int minZ = Math.min(p1.getZ(), p2.getZ());
                                    int maxZ = Math.max(p1.getZ(), p2.getZ());
                                    int minY = Math.min(p1.getY(), p2.getY());

                                    for (int x = minX; x <= maxX; x++) {
                                        for (int z = minZ; z <= maxZ; z++) {
                                            for (int y = 319; y > minY; y--) {
                                                BlockPos pos = new BlockPos(x, y, z);
                                                if (!world.getBlockState(pos).isAir()) {
                                                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                                                }
                                            }
                                        }
                                    }

                                    for (int x = minX; x <= maxX; x++) {
                                        for (int z = minZ; z <= maxZ; z++) {
                                            BlockPos pos = new BlockPos(x, minY, z);
                                            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), 2);
                                        }
                                    }

                                    count++;
                                }

                                final int parcelasRestablecidas = count;
                                source.sendFeedback(() -> Text.literal("✅ Parcelas restablecidas: " + parcelasRestablecidas), true);
                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );
        });
    }
}
