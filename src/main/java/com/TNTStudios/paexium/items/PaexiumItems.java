package com.TNTStudios.paexium.items;

import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class PaexiumItems {

    public static final Item CASCO = new ArmorItem(
            ArmorMaterials.NETHERITE, // Si luego quieres uno personalizado, te ayudo
            ArmorItem.Type.HELMET,
            new Item.Settings().maxCount(1)
    );

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier("paexium", "casco"), CASCO);
    }
}
