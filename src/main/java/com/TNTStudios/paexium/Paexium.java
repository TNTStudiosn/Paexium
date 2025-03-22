package com.TNTStudios.paexium;

import com.TNTStudios.paexium.commands.*;
import com.TNTStudios.paexium.items.PaexiumItems;
import com.TNTStudios.paexium.parcelas.RondaManager;
import net.fabricmc.api.ModInitializer;
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
        CrearRondaCommand.register();
        RondaManager.cargar();
        AsignarParcelasCommand.register();
        VerParcelaCommand.register();
    }
}

