package com.genyo.addon.mixin;

import com.genyo.addon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Friends.class, remap = false)
public class FriendsInjector {

    @Inject(at = @At("RETURN"), method = "add*")
    private void injected(Friend friend, CallbackInfoReturnable<Boolean> cir) {
        if (Enemies.get().get(friend.getName()) != null) {
            Enemies.get().remove(Enemies.get().get(friend.getName()));
        }
    }

}
