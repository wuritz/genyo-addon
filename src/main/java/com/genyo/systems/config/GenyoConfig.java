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

    public final Setting<Boolean> hoverSoundEnabled = sgSounds.add(new BoolSetting.Builder()
        .name("hover-sound")
        .description("Play a sound when hovering over a module in Meteor's GUI.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> hoverVolume = sgSounds.add(new IntSetting.Builder()
        .name("hover-volume")
        .description("Volume of the hover sound.")
        .min(1).defaultValue(60).max(100)
        .sliderRange(1, 100)
        .visible(hoverSoundEnabled::get)
        .build()
    );

    public final Setting<Boolean> clickLeftSoundEnabled = sgSounds.add(new BoolSetting.Builder()
        .name("click-left-sound")
        .description("Play a sound when left-clicking a module in Meteor's GUI.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> clickLeftVolume = sgSounds.add(new IntSetting.Builder()
        .name("click-left-volume")
        .description("Volume of the left-click sound.")
        .min(1).defaultValue(80).max(100)
        .sliderRange(1, 100)
        .visible(clickLeftSoundEnabled::get)
        .build()
    );

    public final Setting<Boolean> clickRightSoundEnabled = sgSounds.add(new BoolSetting.Builder()
        .name("click-right-sound")
        .description("Play a sound when right-clicking a module in Meteor's GUI.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> clickRightVolume = sgSounds.add(new IntSetting.Builder()
        .name("click-right-volume")
        .description("Volume of the right-click sound.")
        .min(1).defaultValue(80).max(100)
        .sliderRange(1, 100)
        .visible(clickRightSoundEnabled::get)
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

    public final Setting<Boolean> vineSound = sgSounds.add(new BoolSetting.Builder()
        .name("vine-sound")
        .description("Play a sound when someone says 'genyo' in chat.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> vineVolume = sgSounds.add(new IntSetting.Builder()
        .name("vine-volume")
        .description("Volume of the vine sound.")
        .min(1).defaultValue(100).max(100)
        .sliderRange(1, 100)
        .visible(vineSound::get)
        .build()
    );

    public final Setting<Boolean> verstappenSound = sgSounds.add(new BoolSetting.Builder()
        .name("verstappen-sound")
        .description("Play a sound when someone says 'verstappen' in chat.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> verstappenVolume = sgSounds.add(new IntSetting.Builder()
        .name("verstappen-volume")
        .description("Volume of the verstappen sound.")
        .min(1).defaultValue(100).max(100)
        .sliderRange(1, 100)
        .visible(verstappenSound::get)
        .build()
    );

    public final Setting<Boolean> kiwiSound = sgSounds.add(new BoolSetting.Builder()
        .name("kiwi-sound")
        .description("Play a sound when someone says 'kiwi' in chat.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> kiwiVolume = sgSounds.add(new IntSetting.Builder()
        .name("kiwi-volume")
        .description("Volume of the kiwi sound.")
        .min(1).defaultValue(100).max(100)
        .sliderRange(1, 100)
        .visible(kiwiSound::get)
        .build()
    );

    public final Setting<Boolean> hamburgerSound = sgSounds.add(new BoolSetting.Builder()
        .name("hamburger-sound")
        .description("Play a sound when an enemy player renders in.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> hamburgerVolume = sgSounds.add(new IntSetting.Builder()
        .name("hamburger-volume")
        .description("Volume of the hamburger sound.")
        .min(1).defaultValue(100).max(100)
        .sliderRange(1, 100)
        .visible(hamburgerSound::get)
        .build()
    );

    public final Setting<Boolean> screamSound = sgSounds.add(new BoolSetting.Builder()
        .name("scream-sound")
        .description("Play a sound when you die.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> screamVolume = sgSounds.add(new IntSetting.Builder()
        .name("scream-volume")
        .description("Volume of the scream sound.")
        .min(1).defaultValue(80).max(100)
        .sliderRange(1, 100)
        .visible(screamSound::get)
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
