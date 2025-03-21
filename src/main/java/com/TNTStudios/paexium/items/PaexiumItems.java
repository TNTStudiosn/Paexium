package com.TNTStudios.paexium.items;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class PaexiumItems {

    public static final Item CASCO = new Item(new Item.Settings().maxCount(1));

    public static final Item PALETA_1 = new Item(new Item.Settings());
    public static final Item PALETA_2 = new Item(new Item.Settings());
    public static final Item PALETA_3 = new Item(new Item.Settings());
    public static final Item PALETA_4 = new Item(new Item.Settings());
    public static final Item PALETA_5 = new Item(new Item.Settings());
    public static final Item PALETA_6 = new Item(new Item.Settings());
    public static final Item PALETA_7 = new Item(new Item.Settings());
    public static final Item PALETA_8 = new Item(new Item.Settings());
    public static final Item PALETA_9 = new Item(new Item.Settings());

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier("paexium", "casco"), CASCO);

        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta1"), PALETA_1);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta2"), PALETA_2);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta3"), PALETA_3);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta4"), PALETA_4);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta5"), PALETA_5);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta6"), PALETA_6);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta7"), PALETA_7);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta8"), PALETA_8);
        Registry.register(Registries.ITEM, new Identifier("paexium", "paleta9"), PALETA_9);
    }
}
