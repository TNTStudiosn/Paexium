package com.TNTStudios.paexium.mixin;

import com.TNTStudios.paexium.items.PaexiumItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Inject(method = "updateItems", at = @At("TAIL"))
    private void paexium$ensureHelmet(CallbackInfo ci) {
        PlayerInventory inv = (PlayerInventory)(Object)this;
        PlayerEntity player = inv.player;

        // Si el jugador no tiene el casco puesto, se lo vuelve a colocar
        ItemStack helmet = inv.getArmorStack(3);
        if (!helmet.isOf(PaexiumItems.CASCO)) {
            inv.armor.set(3, new ItemStack(PaexiumItems.CASCO));
        }

        // Revisar duplicados y eliminarlos del inventario principal
        for (int i = 0; i < inv.main.size(); i++) {
            ItemStack stack = inv.main.get(i);
            if (stack.isOf(PaexiumItems.CASCO)) {
                inv.main.set(i, ItemStack.EMPTY);
            }
        }

        // También en la mano secundaria
        for (int i = 0; i < inv.offHand.size(); i++) {
            ItemStack stack = inv.offHand.get(i);
            if (stack.isOf(PaexiumItems.CASCO)) {
                inv.offHand.set(i, ItemStack.EMPTY);
            }
        }
    }
}
