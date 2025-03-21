package com.TNTStudios.paexium.mixin;

import com.TNTStudios.paexium.items.PaexiumItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void paexium$giveHelmetOnSpawn(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        ItemStack casco = new ItemStack(PaexiumItems.CASCO);

        // Si no lo tiene puesto, lo equipamos
        if (!player.getEquippedStack(EquipmentSlot.HEAD).isOf(PaexiumItems.CASCO)) {
            player.equipStack(EquipmentSlot.HEAD, casco);
        }

        // Limpiar duplicados del inventario
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(PaexiumItems.CASCO)) {
                player.getInventory().removeStack(i);
            }
        }
    }
}
