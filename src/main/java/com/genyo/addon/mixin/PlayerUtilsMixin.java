package com.genyo.addon.mixin;

import com.genyo.addon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerUtils.class, remap = false)
public class PlayerUtilsMixin {

    @Inject(at = @At("TAIL"), method = "getPlayerColor*", cancellable = true)
    private static void injectGetPlayerColor(PlayerEntity entity, Color defaultColor, CallbackInfoReturnable<Color> cir) {
        if (Enemies.get().isEnemy(entity)) {
            cir.setReturnValue(Enemies.get().getEnemyColor());
        }
    }

}
