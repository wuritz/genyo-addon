package com.genyo.systems.modules.misc;

import com.genyo.Genyo;
import com.genyo.systems.modules.GenyoModule;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Blinker extends GenyoModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> renderOriginal = sgGeneral.add(
        new BoolSetting.Builder()
            .name("render-original")
            .description("Spawn Kiwi at initial.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> pulseDelay = sgGeneral.add(
        new IntSetting.Builder()
            .name("pulse-delay")
            .description("0 = disable, >= equals delay between tps")
            .defaultValue(0)
            .min(0)
            .sliderMax(60)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<Keybind> cancelBlink = sgGeneral.add(
        new KeybindSetting.Builder()
            .name("cancel-blink")
            .description("cancel tp and return to initial kiwi.")
            .defaultValue(Keybind.none())
            .action(() -> {
                cancelled = true;
                disable();
            })
            .build()
    );
    private final List<Packet<?>> packets = new ArrayList<>();

    private FakePlayerEntity model;
    private Vec3d startPos = Vec3d.ZERO;
    private boolean sending;
    private boolean cancelled;

    private int timer;

    public Blinker() {
        super(
            Genyo.MISC,
            "Tphaxx",
            "You have to be mentally ready before using this ultimate tphaxx module"
        );
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate() || mc.player == null) return;

        cancelled = false;
        timer = 0;

        synchronized (packets) {
            packets.clear();
        }
        startPos = new Vec3d(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
        );

        if (renderOriginal.get()) {
            model = new FakePlayerEntity(
                mc.player,
                mc.player.getGameProfile().name(),
                20,
                true
            );

            model.doNotPush = true;
            model.hideWhenInsideCamera = true;
            model.noHit = true;
            model.spawn();
        }
    }

    @Override
    public void onDeactivate() {
        if (!Utils.canUpdate() || mc.player == null) return;
        dumpPackets(!cancelled);

        if (cancelled) {
            mc.player.setPosition(startPos);
            mc.player.setVelocity(Vec3d.ZERO);
        }

        cancelled = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate() || mc.player == null) return;

        timer++;

        if (pulseDelay.get() > 0 && timer >= pulseDelay.get()) {
            dumpPackets(true);
            startBuffering();
        }
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!Utils.canUpdate()) return;
        if (sending) return;

        Packet<?> packet = event.packet;

        synchronized (packets) {
            packets.add(packet);
        }
        event.cancel();
    }

    @EventHandler
    private void onJoinGame(GameJoinedEvent event) {
        if (isActive()) {
            warning(
                "tphaxx is active!!!. Outgoing packets are being buffered."
            );
        }
    }

    @EventHandler
    private void onLeaveGame(GameLeftEvent event) {
        dumpPackets(false);
        cancelled = false;
    }

    @Override
    public String getInfoString() {
        int packetCount;

        synchronized (packets) {
            packetCount = packets.size();
        }

        return String.format(
            "%.1fs | %d packets",
            timer / 20.0f,
            packetCount
        );
    }
    private void startBuffering() {
        if (!Utils.canUpdate() || mc.player == null) return;

        timer = 0;
        cancelled = false;

        synchronized (packets) {
            packets.clear();
        }

        startPos = new Vec3d(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ()
        );

        if (renderOriginal.get()) {
            if (model != null) {
                model.despawn();
            }

            model = new FakePlayerEntity(
                mc.player,
                mc.player.getGameProfile().name(),
                20,
                true
            );

            model.doNotPush = true;
            model.hideWhenInsideCamera = true;
            model.noHit = true;
            model.spawn();
        }
    }
    private void dumpPackets(boolean send) {
        sending = true;
        List<Packet<?>> queuedPackets;
        synchronized (packets) {
            queuedPackets = new ArrayList<>(packets);
            packets.clear();
        }

        if (send && mc.getNetworkHandler() != null) {
            for (Packet<?> packet : queuedPackets) {
                mc.getNetworkHandler()
                    .getConnection()
                    .send(packet);
            }
        }

        sending = false;

        if (model != null) {
            model.despawn();
            model = null;
        }

        timer = 0;
    }
}
