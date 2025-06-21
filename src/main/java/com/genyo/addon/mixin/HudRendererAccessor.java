package com.genyo.addon.mixin;

import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HudRenderer.class)
public interface HudRendererAccessor {

    @Accessor(value = "INSTANCE", remap = false)
    HudRenderer getHudRenderer();

}
