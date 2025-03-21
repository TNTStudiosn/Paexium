package com.TNTStudios.paexium.mixin;

import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Explosion.class)
public class ExplosionMixin {

    @Overwrite
    public boolean shouldDestroy() {
        return false; // Nunca destruye bloques
    }
}
