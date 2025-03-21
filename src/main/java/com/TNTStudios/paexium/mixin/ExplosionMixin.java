package com.TNTStudios.paexium.mixin;

import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Explosion.class)
public class ExplosionMixin {

    /**
     * @author TNTStudios
     * @reason Queremos evitar que cualquier explosión (TNT, creepers, etc.)
     * destruya bloques del mundo. Este método sobrescribe el comportamiento
     * original de la clase Explosion para impedir la destrucción de bloques.
     */
    @Overwrite
    public boolean shouldDestroy() {
        return false; // Nunca destruye bloques
    }

}
