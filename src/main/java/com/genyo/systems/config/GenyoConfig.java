package com.genyo.systems.config;

import com.genyo.Genyo;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.NbtCompound;

public class GenyoConfig extends System<GenyoConfig> {

    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSounds = settings.createGroup("Sounds");
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    public boolean visibleDiscord = false;

    // General

    public final Setting<TextPosition> textPosition = sgGeneral.add(new EnumSetting.Builder<TextPosition>()
        .name("text-position")
        .description("Position of the Title Screen text")
        .defaultValue(TextPosition.Top)
        .build()
    );

    public final Setting<Boolean> genyoDiscord = sgGeneral.add(new BoolSetting.Builder()
        .name("Force Genyo Discord")
        .description("You can't turn off Genyo Discord if this is turned on.")
        .defaultValue(true)
        .visible(this::forceDiscordVisible)
        .build()
    );

    // Sounds

    public final Setting<Integer> globalVolume = sgSounds.add(new IntSetting.Builder()
        .name("global-volume")
        .description("Adjust the global volume of Genyo sounds.")
        .sliderRange(10, 100)
        .min(10).defaultValue(100).max(100)
        .build()
    );

    public final Setting<Boolean> guiSounds = sgSounds.add(new BoolSetting.Builder()
        .name("gui-sounds")
        .description("Play sounds when hovering and clicking in Meteor's GUI.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> hoverVolume = sgSounds.add(new IntSetting.Builder()
        .name("hover-volume")
        .description("Volume of the hover sound.")
        .min(1).defaultValue(60).max(100)
        .sliderRange(1, 100)
        .visible(guiSounds::get)
        .build()
    );

    public final Setting<Integer> clickVolume = sgSounds.add(new IntSetting.Builder()
        .name("click-volume")
        .description("Volume of left/right click sounds.")
        .min(1).defaultValue(80).max(100)
        .sliderRange(1, 100)
        .visible(guiSounds::get)
        .build()
    );

    public final Setting<Boolean> blackPerson = sgSounds.add(new BoolSetting.Builder()
        .name("black-person")
        .description("Detect when black person")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> typing = sgSounds.add(new BoolSetting.Builder()
        .name("typing")
        .description("Typing sound in chat")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> typingVolume = sgSounds.add(new IntSetting.Builder()
        .name("typing-volume")
        .description("Adjust the volume of the typing sound")
        .min(1).defaultValue(80).max(100)
        .sliderRange(1, 100)
        .visible(typing::get)
        .build()
    );

    // Visual

    public final Setting<Boolean> useGenyoSplashes = sgVisual.add(new BoolSetting.Builder()
        .name("use-genyo-splashes")
        .description("Use Genyo's custom splash texts in the title screen.")
        .defaultValue(true)
        .build()
    );

    public GenyoConfig() {
        super("genyo-config");
    }

    public static GenyoConfig get() {
        return Systems.get(GenyoConfig.class);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("version", Genyo.VERSION.toString());
        tag.put("settings", settings.toTag());
        if (genyoDiscord.isVisible() || visibleDiscord) tag.putBoolean("forceDiscord", genyoDiscord.get());

        return tag;
    }

    @Override
    public GenyoConfig fromTag(NbtCompound tag) {
        if (tag.contains("settings")) tag.getCompound("settings").ifPresent(settings::fromTag);
        if (tag.contains("forceDiscord")) tag.getBoolean("forceDiscord").ifPresent(value -> {
            visibleDiscord = true;
        });

        return this;
    }

    public void makeDiscordVisible() {
        visibleDiscord = true;
        genyoDiscord.reset();
        genyoDiscord.set(false);
    }

    private boolean forceDiscordVisible() {
        return visibleDiscord;
    }

    public enum TextPosition {
        Top, Center
    }

}
