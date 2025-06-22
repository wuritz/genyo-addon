package com.genyo.addon.systems.enemies;

import com.genyo.addon.GenyoAddon;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;

public class Enemies extends System<Enemies> implements Iterable<Enemy> {

    private final List<Enemy> enemies = new ArrayList<>();

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMessage = settings.createGroup("Message");

    private final Setting<SettingColor> enemyColor = sgGeneral.add(new ColorSetting.Builder()
        .name("esp-color")
        .description("Brasil")
        .defaultValue(new SettingColor(255, 100, 89, 255))
        .build()
    );

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("Keybind")
        .description("Ecuadór")
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_X))
        .action(this::genyo)
        .build()
    );

    // Message

    private final Setting<Boolean> message = sgMessage.add(new BoolSetting.Builder()
        .name("send-message")
        .description("Megüzeni a kiválasztott emberpajtásunknak, hogy mi a téma.")
        .defaultValue(false)
        .build()
    );

    private final Setting<MessageMode> messageMode = sgMessage.add(new EnumSetting.Builder<MessageMode>()
        .name("message-mode")
        .description("Shabala babala /whisper oder /msg")
        .defaultValue(MessageMode.Whisper)
        .build()
    );

    private final Setting<String> customMessage = sgMessage.add(new StringSetting.Builder()
        .name("custom-message")
        .description("Hajrá Mezőkövesd! :D :D :D")
        .defaultValue("shabala babala niger biger.")
        .build()
    );

    public Enemies() {
        super("enemies");
    }

    public static Enemies get() {
        return Systems.get(Enemies.class);
    }

    public boolean add(Enemy enemy) {
        if (enemy.name.isEmpty() || enemy.name.contains(" ")) return false;

        if (!enemies.contains(enemy)) {
            if (Friends.get().get(enemy.getName()) != null) {
                Friends.get().remove(Friends.get().get(enemy.getName()));
            }

            enemies.add(enemy);
            save();

            return true;
        }

        return false;
    }

    public boolean remove(Enemy enemy) {
        if (enemies.remove(enemy)) {
            save();
            return true;
        }

        return false;
    }

    public Enemy get(String name) {
        for (Enemy enemy : enemies) {
            if (enemy.name.equalsIgnoreCase(name)) {
                return enemy;
            }
        }

        return null;
    }

    public Enemy get(PlayerEntity player) {
        return get(player.getName().getString());
    }

    public Enemy get(PlayerListEntry player) {
        return get(player.getProfile().getName());
    }

    public boolean isEnemy(PlayerEntity player) {
        return player != null && get(player) != null;
    }

    public boolean isEnemy(PlayerListEntry player) {
        return get(player) != null;
    }

    public boolean shouldAttack(PlayerEntity player) {
        return isEnemy(player);
    }

    public int count() {
        return enemies.size();
    }

    public boolean isEmpty() {
        return enemies.isEmpty();
    }

    @Override
    public @NotNull Iterator<Enemy> iterator() {
        return enemies.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("friends", NbtUtils.listToTag(enemies));
        tag.put("settings", settings.toTag());

        return tag;
    }

    private void genyo() {
        GenyoAddon.LOG.info("fa");

        if (mc.targetedEntity == null) return;
        if (!(mc.targetedEntity instanceof PlayerEntity player)) return;

        if (!isEnemy(player)) {
            add(new Enemy(player));
            info("Added %s to enemies", player.getName().getString());
            if (getMessageBool()) {
                String message = getCustomMessage();

                switch (getMessageMode()) {
                    case MessageMode.Whisper:
                        ChatUtils.sendPlayerMsg("/whisper " + player.getName().getString() + " " + message);
                        GenyoAddon.LOG.info("/whisper " + player.getName().getString() + " " + message);
                    case MessageMode.Msg:
                        ChatUtils.sendPlayerMsg("/msg " + player.getName().getString() + " " + message);
                }
            }
        } else {
            remove(get(player));
            info("Removed %s from opps.", player.getName().getString());
        }
    }

    @Override
    public Enemies fromTag(NbtCompound tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));

        enemies.clear();

        for (NbtElement itemTag : tag.getList("enemies", 0)) {
            NbtCompound enemyTag = (NbtCompound) itemTag;
            if (!enemyTag.contains("name")) continue;

            String name = enemyTag.getString("name");
            if (get(name) != null) continue;

            String uuid = enemyTag.getString("id");
            Enemy enemy = !uuid.isBlank()
                ? new Enemy(name, UndashedUuid.fromStringLenient(uuid))
                : new Enemy(name);

            enemies.add(enemy);
        }

        Collections.sort(enemies);

        MeteorExecutor.execute(() -> enemies.forEach(Enemy::updateInfo));

        return this;
    }

    public Color getEnemyColor() {
        return enemyColor.get();
    }

    public Keybind getKeybind() {
        return keybind.get();
    }

    public boolean getMessageBool() {
        return message.get();
    }

    public String getCustomMessage() {
        return customMessage.get();
    }

    public MessageMode getMessageMode() {
        return messageMode.get();
    }

    public enum MessageMode {
        Whisper,
        Msg
    }
}
