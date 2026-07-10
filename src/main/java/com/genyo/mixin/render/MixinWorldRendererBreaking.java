package com.genyo.mixin.render;

import com.genyo.systems.modules.visual.GenyoNoMineAnimation;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRendererBreaking {

    @Inject(method = "setBlockBreakingInfo", at = @At("HEAD"), cancellable = true)
    private void genyo$hookSetBlockBreakingInfo(int breakerId, BlockPos pos, int stage, CallbackInfo ci) {
        GenyoNoMineAnimation module = Modules.get().get(GenyoNoMineAnimation.class);
        if (module != null && module.shouldCancel(breakerId)) {
            ci.cancel();
        }
    }
}
