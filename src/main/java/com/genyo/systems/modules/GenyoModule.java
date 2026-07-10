package com.genyo.systems.modules;

import com.genyo.managers.Managers;
import com.genyo.managers.player.rotation.Rotation;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GenyoModule extends Module {

    protected Random RANDOM = ThreadLocalRandom.current();

    private final String prefix = Formatting.GOLD + "" + Formatting.BOLD + "[Genyo]";

    public GenyoModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public static boolean fullNullCheck() {
        return MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null;
    }

    //  Messages
    public void sendToggledMsg() {
        if (!Config.get().chatFeedback.get() && !chatFeedback && mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + title + (isActive() ? Formatting.GREEN + " ON" : Formatting.RED + " OFF");
        sendMessage(Text.of(msg), hashCode());
    }

    public void sendToggledMsg(String message) {
        if (!Config.get().chatFeedback.get() && !chatFeedback && mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + title + (isActive() ? Formatting.GREEN + " ON " : Formatting.RED + " OFF ") + Formatting.GRAY + message;
        sendMessage(Text.of(msg), hashCode());
    }

    public void sendDisableMsg(String text) {
        if (mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + title + Formatting.RED + " OFF " + Formatting.GRAY + "- " + text;
        sendMessage(Text.of(msg), hashCode());
    }

    public void sendInfo(String text) {
        if (mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + title + " - " + Formatting.GRAY + text;
        sendMessage(Text.of(msg), Objects.hash(name + "-info"));
    }

    public void sendError(String text) {
        if (mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.RED + title + " - " + text;
        sendMessage(Text.of(msg), hashCode());
    }

    public void debug(String text) {
        if (mc.world == null) return;

        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + name + " " + Formatting.AQUA + text;
        sendMessage(Text.of(msg), 0);
    }

    public void sendMessage(Text text, int id) {
        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(text, id);
    }

    public void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    protected void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;

        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence();) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }

    protected void setRotation(float yaw, float pitch)
    {
        Managers.ROTATION.setRotation(new Rotation(100, yaw, pitch));
    }

    protected boolean isRotationBlocked()
    {
        return Managers.ROTATION.isRotationBlocked(100);
    }

    protected void setRotationSilent(float yaw, float pitch)
    {
        Managers.ROTATION.setRotationSilent(yaw, pitch);
    }

    public void sendSequenced(SequencedPacketCreator packetCreator) {
        if (mc.interactionManager == null || mc.world == null || mc.getNetworkHandler() == null) return;

        PendingUpdateManager sequence = mc.world.getPendingUpdateManager().incrementSequence();
        Packet<?> packet = packetCreator.predict(sequence.getSequence());

        mc.getNetworkHandler().sendPacket(packet);

        sequence.close();
    }

    public Setting<Boolean> addPauseEat(SettingGroup group) {
        return group.add(new BoolSetting.Builder()
            .name("Pause Eat")
            .description("Pauses when eating")
            .defaultValue(false)
            .build()
        );
    }

    public PlayerEntity getClosestPlayer(double range) {
        return mc.world.getPlayers().stream().filter(e -> !(e instanceof ClientPlayerEntity) && !e.isSpectator())
            .filter(e -> mc.player.squaredDistanceTo(e) <= range * range)
            .filter(e -> !Friends.get().isFriend(e))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e))).orElse(null);
    }

    public boolean checkMultitask() {
        return checkMultitask(false);
    }

    public boolean checkMultitask(boolean checkOffhand) {
        if (checkOffhand && mc.player.getActiveHand() != Hand.MAIN_HAND) {
            return false;
        }
        return mc.player.isUsingItem();
    }
}
