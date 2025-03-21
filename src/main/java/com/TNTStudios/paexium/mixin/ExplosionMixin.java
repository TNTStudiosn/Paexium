package com.TNTStudios.paexium.mixin;

import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Explosion.class)
public class ExplosionMixin {

    /**
     * Evita que cualquier explosión destruya bloques en el mundo.
     * Esto incluye TNT, Creepers, Ghasts, etc.
     */
    @Overwrite
    public boolean shouldDestroy() {
        return false; // Nunca destruye bloques
    }

}
