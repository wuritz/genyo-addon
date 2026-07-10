package com.genyo.systems.modules.visual;

import com.genyo.Genyo;
import com.genyo.render.animation.Animation;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenyoLogoutPoints extends Module {

    private static final DecimalFormat DECIMAL = new DecimalFormat("#.#");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows distance from the logout.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showTimePassed = sgGeneral.add(new BoolSetting.Builder()
        .name("time-passed")
        .description("Shows time passed since logout.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> fadeTime = sgGeneral.add(new IntSetting.Builder()
        .name("fade-time")
        .description("How long the fade in/out animation takes, in milliseconds.")
        .min(0)
        .defaultValue(500)
        .max(2000)
        .build()
    );

    private final Setting<SettingColor> boxColor = sgGeneral.add(new ColorSetting.Builder()
        .name("box-color")
        .description("Color of the logout marker outline.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the logout tag text.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Map<UUID, LogoutData> onlinePlayers = new ConcurrentHashMap<>();
    private final Map<UUID, LogoutData> offlinePlayers = new ConcurrentHashMap<>();

    public GenyoLogoutPoints() {
        super(Genyo.VISUAL, "genyo-logout-points", "Marks nearby logouts.");
    }

    @Override
    public void onDeactivate() {
        onlinePlayers.clear();
        offlinePlayers.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != null && player.getGameProfile() != null && !player.equals(mc.player)) {
                onlinePlayers.put(player.getGameProfile().id(), new LogoutData(player, fadeTime.get()));
            }
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.packet instanceof PlayerListS2CPacket packet
            && packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
            for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
                LogoutData data = offlinePlayers.get(entry.profile().id());
                if (data != null) {
                    data.setState(false);
                }
            }

            onlinePlayers.clear();
        } else if (event.packet instanceof PlayerRemoveS2CPacket packet) {
            List<UUID> profileIds = packet.profileIds();
            for (UUID uuid : profileIds) {
                LogoutData data = onlinePlayers.get(uuid);
                if (data == null) {
                    continue;
                }

                if (!offlinePlayers.containsKey(uuid)) {
                    offlinePlayers.put(uuid, data);
                    data.setState(true);
                }
            }

            onlinePlayers.clear();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (Map.Entry<UUID, LogoutData> set : offlinePlayers.entrySet()) {
            LogoutData data = set.getValue();
            if (data == null) {
                continue;
            }

            double factor = data.getAnimation().getFactor();
            if (!data.getAnimation().getState() && factor <= 0.01) {
                offlinePlayers.remove(set.getKey());
                continue;
            }

            PlayerEntity offlinePlayer = data.getOfflinePlayer();
            Box box = offlinePlayer.getBoundingBox();

            Color lineColor = boxColor.get().a((int) (boxColor.get().a * factor));
            Color sideColor = lineColor.a((int) (lineColor.a * 0.4));

            event.renderer.box(
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ,
                sideColor, lineColor, ShapeMode.Lines, 0
            );
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (LogoutData data : offlinePlayers.values()) {
            double factor = data.getAnimation().getFactor();
            if (factor <= 0.01) {
                continue;
            }

            PlayerEntity offlinePlayer = data.getOfflinePlayer();
            Box box = offlinePlayer.getBoundingBox();

            StringBuilder logoutTag = new StringBuilder(offlinePlayer.getName().getString() + "'s Logout");

            boolean hasExtraInfo = false;
            if (showDistance.get()) {
                hasExtraInfo = true;
                double dist = Math.sqrt(mc.player.squaredDistanceTo(offlinePlayer.getEntityPos()));
                logoutTag.append(" - ").append(DECIMAL.format(dist)).append("m");
            }

            if (showTimePassed.get()) {
                if (!hasExtraInfo) {
                    logoutTag.append(" -");
                }

                long seconds = (System.currentTimeMillis() - data.getLogoutTime()) / 1000;
                if (seconds < 60) {
                    logoutTag.append(" ").append(seconds).append("s");
                } else {
                    long minutes = seconds / 60;
                    logoutTag.append(" ").append(minutes).append("min");
                }
            }

            Vec3d center = box.getCenter();
            Vector3d pos3d = new Vector3d(center.x, box.maxY + 0.2, center.z);
            Vector3d pos2d = new Vector3d(pos3d);

            if (NametagUtils.to2D(pos2d, 1.5)) {
                Color currentTextColor = textColor.get().a((int) (textColor.get().a * factor));
                String tag = logoutTag.toString();

                NametagUtils.begin(pos3d);
                TextRenderer.get().begin(1.0, false, true);
                TextRenderer.get().render(tag, -TextRenderer.get().getWidth(tag) / 2.0, 0, currentTextColor, true);
                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    private static class LogoutData {
        private final PlayerEntity offlinePlayer;
        private final long logoutTime;
        private final Animation animation;

        public LogoutData(PlayerEntity offlinePlayer, float fadeLength) {
            this.offlinePlayer = offlinePlayer;
            this.logoutTime = System.currentTimeMillis();
            this.animation = new Animation(false, fadeLength);
        }

        public PlayerEntity getOfflinePlayer() {
            return offlinePlayer;
        }

        public long getLogoutTime() {
            return logoutTime;
        }

        public Animation getAnimation() {
            return animation;
        }

        public void setState(boolean state) {
            this.animation.setState(state);
        }
    }
}
