package com.genyo.addon.hud;

import com.genyo.addon.GenyoAddon;
import com.genyo.addon.utils.HudUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class PvPNeccessaryHud extends HudElement {

    public static final HudElementInfo<PvPNeccessaryHud> INFO = new HudElementInfo<>(GenyoAddon.HUD_GROUP, "pvp-neccessary", "Fasz fasz fasz fasz fasz fasz.", PvPNeccessaryHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Itemek amiket kiír.")
        .defaultValue(Items.TOTEM_OF_UNDYING, Items.ENDER_PEARL, Items.END_CRYSTAL, Items.OBSIDIAN)
        .build()
    );

    private final Setting<NoneMode> noneMode = sgGeneral.add(new EnumSetting.Builder<NoneMode>()
        .name("none-mode")
        .description("How to render the item when you don't have the specified item in your inventory.")
        .defaultValue(NoneMode.ShowCount)
        .build()
    );

    // Scale

    public final Setting<Integer> margin = sgScale.add(new IntSetting.Builder()
            .name("margin")
            .description("Két dolog közötti hely")
            .defaultValue(0)
            .onChanged(aInt -> calculateSize())
            .min(0)
            .sliderRange(0, 10)
            .build()
    );

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(aBoolean -> calculateSize())
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

    public final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private PvPNeccessaryHud() {
        super(INFO);

        calculateSize();
    }

    private void calculateSize() {
        int offset = items.get().size();
        setSize(21 * getScale() * offset, 17 * getScale() + 20);
    }

    @Override
    public void render(HudRenderer renderer) {
        calculateSize();
        int itemsLength = items.get().size();

        if (noneMode.get() == NoneMode.HideItem) {
            if (isInEditor()) {
                renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
                renderer.line(x, y + getHeight(), x + getWidth(), y, Color.GRAY);
            }
        } else {
            for  (int i = 0; i < itemsLength; i++) {
                Item item = items.get().get(i);
                ItemStack itemStack = new ItemStack(item, InvUtils.find(item).count());

                int scaleOffset = (int) (getScale() * 10);
                int offset = i+1 != 1 ? i * 50 * scaleOffset / (20 - margin.get()) : 0;

                renderer.post(() -> render(renderer, itemStack, x + offset, y));
                //((HudRendererAccessor) renderer).getHudRenderer().post(() -> render(renderer, itemStack, x + offset, y));
            }
        }

        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
    }

    private void render(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        if (noneMode.get() == NoneMode.HideItem) {
            renderer.item(itemStack, x, y, getScale(), true);
            return;
        }

        String countOverride = null;
        boolean resetToZero = false;

        countOverride = String.valueOf(itemStack.getCount());

        if (itemStack.getCount() == 1) {
            countOverride = "1";
        }

        if (itemStack.isEmpty()) {
            if (noneMode.get() == NoneMode.ShowCount)
                countOverride = "0";

            itemStack.setCount(1);
            resetToZero = true;
        }

        //renderer.item(itemStack, x, y, getScale(), true, countOverride);
        HudUtils.drawItem(renderer.drawContext, itemStack, x, y, getScale(), countOverride);

        if (resetToZero)
            itemStack.setCount(0);
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }

    public enum NoneMode {
        HideItem,
        HideCount,
        ShowCount;

        @Override
        public String toString() {
            return switch (this) {
                case HideItem -> "Hide Item";
                case HideCount -> "Hide Count";
                case ShowCount -> "Show Count";
            };
        }
    }

}
