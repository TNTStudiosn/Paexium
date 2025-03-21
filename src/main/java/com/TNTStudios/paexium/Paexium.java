package com.TNTStudios.paexium;

import com.TNTStudios.paexium.items.PaexiumItems;
import net.fabricmc.api.ModInitializer;

public class Paexium implements ModInitializer {

    @Override
    public void onInitialize() {
        PaexiumItems.register();
    }
}
