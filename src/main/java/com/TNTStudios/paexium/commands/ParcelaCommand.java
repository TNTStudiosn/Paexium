package com.TNTStudios.paexium.commands;

import com.TNTStudios.paexium.parcelas.ParcelManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.WorldEdit;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ParcelaCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(CommandManager.literal("CrearParcela")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("numero", IntegerArgumentType.integer())
                            .executes(context -> {
                                int id = IntegerArgumentType.getInteger(context, "numero");
                                ServerCommandSource source = context.getSource();
                                ServerPlayerEntity player = source.getPlayer();

                                Actor actor = FabricAdapter.adaptPlayer(player);
                                LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
                                Region region;

                                try {
                                    region = session.getSelection(FabricAdapter.adapt(player.getWorld()));
                                } catch (Exception e) {
                                    source.sendError(Text.literal("❌ Debes seleccionar una región con WorldEdit."));
                                    return 0;
                                }

                                if (!(region instanceof CuboidRegion cuboid)) {
                                    source.sendError(Text.literal("❌ La selección debe ser cúbica (Cuboid)."));
                                    return 0;
                                }

                                BlockPos pos1 = new BlockPos(
                                        cuboid.getMinimumPoint().getBlockX(),
                                        cuboid.getMinimumPoint().getBlockY(),
                                        cuboid.getMinimumPoint().getBlockZ()
                                );

                                BlockPos pos2 = new BlockPos(
                                        cuboid.getMaximumPoint().getBlockX(),
                                        cuboid.getMaximumPoint().getBlockY(),
                                        cuboid.getMaximumPoint().getBlockZ()
                                );

                                ParcelManager.saveParcela(id, pos1, pos2);
                                source.sendFeedback(() -> Text.literal("✅ Parcela " + id + " registrada correctamente."), false);

                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );
        });
    }
}
