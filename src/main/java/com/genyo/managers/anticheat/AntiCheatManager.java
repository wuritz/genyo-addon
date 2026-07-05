package com.genyo.managers.anticheat;

import com.genyo.events.network.DisconnectEvent;
import com.genyo.utils.GenyoChatUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AntiCheatManager {

    private SetbackData lastSetback;

    private final int[] transactions = new int[4];
    private int index;
    private boolean isGrim;

    public AntiCheatManager() {
        Arrays.fill(transactions, -1);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof CommonPingS2CPacket packet)
        {
            if (index > 3) return;

            final int uid = packet.getParameter();
            transactions[index] = uid;
            ++index;
            if (index == 4) grimCheck();
        } else if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            Vec3d oldPos = packet.change().position();
            lastSetback = new SetbackData(new Vec3d(oldPos.getX(), oldPos.getY(), oldPos.getZ()),
                System.currentTimeMillis(), packet.teleportId());
        }
    }

    @EventHandler
    public void onDisconnect(final DisconnectEvent event) {
        Arrays.fill(transactions, -1);
        index = 0;
        isGrim = false;
    }

    private void grimCheck()
    {
        boolean detected = true;
        for (int i = 0; i < 4; ++i)
        {
            if (transactions[i] != -i)
            {
                detected = false;
                break;
            }
        }

        // Only announce once, and only run the chat/HUD call on the main
        // thread since this handler runs on the Netty IO thread and
        // GenyoChatUtils ends up touching render code (ChatHud -> GL calls).
        if (detected && !isGrim)
        {
            isGrim = true;
            mc.execute(() -> GenyoChatUtils.sendInfo("Server is running GrimAC"));
        }
    }

    public boolean isGrim()
    {
        return isGrim;
    }

    public boolean hasPassed(final long timeMS)
    {
        return lastSetback != null && lastSetback.timeSince() >= timeMS;
    }

}
