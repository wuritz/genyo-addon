package com.genyo.systems.modules.visual;

import com.genyo.Genyo;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class NoSwing extends Module {

    public NoSwing() {
        super(Genyo.VISUAL, "no-swing", "NoSwing u know use your brain mr.");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof HandSwingC2SPacket) {
            event.cancel();
        }
    }
}
