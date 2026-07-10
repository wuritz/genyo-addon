package com.genyo.mixin.accessor;

import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CrystalAura.class)
public interface AccessorCrystalAura {
    @Accessor("placing")
    boolean isPlacing();
}
