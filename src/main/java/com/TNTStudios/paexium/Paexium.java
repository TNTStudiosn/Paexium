package com.TNTStudios.paexium;

import com.TNTStudios.paexium.commands.ResetParcelasCommand;
import com.TNTStudios.paexium.commands.RuletaCommand;
import com.TNTStudios.paexium.commands.TeleportParcelaCommand;
import com.TNTStudios.paexium.items.PaexiumItems;
import net.fabricmc.api.ModInitializer;
import com.TNTStudios.paexium.commands.ParcelaCommand;
import com.TNTStudios.paexium.parcelas.ParcelManager;

public class Paexium implements ModInitializer {

    @Override
    public void onInitialize() {
        PaexiumItems.register();
        RuletaCommand.register();
        ParcelaCommand.register();
        ParcelManager.load();
        TeleportParcelaCommand.register();
        ResetParcelasCommand.register();
    }
}
