package com.genyo.managers.network;

import com.genyo.systems.config.GenyoConfig;
import com.genyo.systems.modules.misc.GenyoDiscord;
import com.genyo.utils.GenyoChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public class GDTogglerManager {

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Modules.get().isActive(GenyoDiscord.class) && GenyoConfig.get().genyoDiscord.get()) {
            Modules.get().get(GenyoDiscord.class).toggle();
            GenyoChatUtils.sendInfo("Why would you turn it off?");
        }
    }

}
