package com.genyo.systems.modules.visual;



import com.genyo.Genyo;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

public class GenyoNoMineAnimation extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Whose block-breaking animation to hide.")
        .defaultValue(Mode.Everyone)
        .build()
    );

    private final Setting<Boolean> hideParticles = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-particles")
        .description("Also hides the block-breaking particles.")
        .defaultValue(true)
        .build()
    );

    public GenyoNoMineAnimation() {
        super(Genyo.VISUAL, "genyo-no-mine-animation", "Hides the block-breaking (crack) animation.");
    }

    // Used by the mixin to decide whether to cancel a given breaking-info update.
    public boolean shouldCancel(int breakerId) {
        if (!isActive()) return false;

        boolean isSelf = mc.player != null && breakerId == mc.player.getId();

        return switch (mode.get()) {
            case Everyone -> true;
            case OthersOnly -> !isSelf;
            case SelfOnly -> isSelf;
        };
    }

    public boolean hideParticles() {
        return isActive() && hideParticles.get();
    }

    public enum Mode {
        Everyone, OthersOnly, SelfOnly
    }
}
