package com.genyo.addon.hud;

import com.genyo.addon.GenyoAddon;
import com.genyo.addon.utils.HudUtils;
import com.genyo.addon.utils.InventoryUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
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

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Brasil")
        .defaultValue(new SettingColor(255, 255, 255, 255))
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
        setSize(23 * getScale() * offset, 17 * getScale() + 20);
    }

    @Override
    public void render(HudRenderer renderer) {
        calculateSize();
        int itemsLength = items.get().size();

        for  (int i = 0; i < itemsLength; i++) {
            Item item = items.get().get(i);

            ItemStack itemStack = new ItemStack(item, InventoryUtils.find(item).count());

            int scaleOffset = (int) (getScale() * 10);
            int intScale = (int) (getScale());
            int offset = i+1 != 1 ? i * 50 * scaleOffset / (20 - margin.get()) : 0;

            int textXOffset = 6 * intScale;
            int textYOffset = 17 * intScale;

            if (itemStack.getCount() > 100) {
                textXOffset -= 6 * intScale;
            } else if (itemStack.getCount() > 10) {
                textXOffset -= 2 * intScale;
            }

            int finalTextXOffset = textXOffset;
            renderer.post(() -> {
                render(renderer, itemStack, x + offset, y);
                renderText(renderer, itemStack, x + offset + finalTextXOffset, y + textYOffset);
            });
        }

        if (background.get()) renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
    }

    private void render(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        boolean resetToZero = false;

        if (itemStack.isEmpty()) {
            itemStack.setCount(1);
            resetToZero = true;
        }

        HudUtils.drawItem(renderer.drawContext, itemStack, x, y, getScale());

        if (resetToZero)
            itemStack.setCount(0);
    }

    private void renderText(HudRenderer renderer, ItemStack itemStack, int x, int y) {
        String count = String.valueOf(itemStack.getCount());

        if (itemStack.getCount() == 1) {
            count = "1";
        }

        boolean resetToZero = false;

        if (itemStack.isEmpty()) {
            itemStack.setCount(1);
            resetToZero = true;
        }

        renderer.text(count, x, y, textColor.get(), true, getScale() / 2);

        if (resetToZero)
            itemStack.setCount(0);
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }

}
