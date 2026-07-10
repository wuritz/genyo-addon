package com.genyo.mixin.accessor;

import meteordevelopment.meteorclient.systems.modules.render.LogoutSpots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LogoutSpots.class)
public interface AccessorLogoutSpots {
    @Accessor("players")
    List<Object> getPlayers();
}
