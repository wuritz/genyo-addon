package com.genyo.mixin.meteor;

import com.genyo.managers.Managers;
import com.genyo.core.sound.SoundManager;
import com.genyo.systems.config.GenyoConfig;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorModule;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.gui.Click;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WWidget.class, remap = false)
public class MixinWWidget {

    @Shadow public boolean mouseOver;
    @Unique private boolean genyo_wasOver = false;

    @Inject(method = "mouseMoved", at = @At("TAIL"))
    private void onMouseMoved(double mouseX, double mouseY,
                              double lastMouseX, double lastMouseY, CallbackInfo ci) {
        // Only fire on module buttons
        if (!(((Object) this) instanceof WMeteorModule)) return;

        if (mouseOver && !genyo_wasOver) {
            GenyoConfig cfg = GenyoConfig.get();
            if (cfg != null && cfg.hoverSoundEnabled.get()) {
                float vol = cfg.hoverVolume.get() / 100f;
                Managers.SOUND.playUISound(SoundManager.GUI_HOVER, vol, 1f);
            }
        }
        genyo_wasOver = mouseOver;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(Click click, boolean doubled,
                                CallbackInfoReturnable<Boolean> cir) {
        // Only fire on module buttons
        if (!(((Object) this) instanceof WMeteorModule)) return;
        if (!mouseOver) return;

        GenyoConfig cfg = GenyoConfig.get();
        if (cfg == null) return;

        if (click.button() == 0) {
            if (cfg.clickLeftSoundEnabled.get()) {
                float vol = cfg.clickLeftVolume.get() / 100f;
                Managers.SOUND.playUISound(SoundManager.GUI_CLICK_LEFT, vol, 1f);
            }
        } else if (click.button() == 1) {
            if (cfg.clickRightSoundEnabled.get()) {
                float vol = cfg.clickRightVolume.get() / 100f;
                Managers.SOUND.playUISound(SoundManager.GUI_CLICK_RIGHT, vol, 1f);
            }
        }
    }
}
