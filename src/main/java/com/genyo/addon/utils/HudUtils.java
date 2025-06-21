package com.genyo.addon.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HudUtils {

    public static void drawItem(DrawContext drawContext, ItemStack itemStack, int x, int y, float scale, String countOverride) {
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1f);
        matrices.translate(0, 0, 401); // Thanks Mojang

        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        drawContext.drawItem(itemStack, scaledX, scaledY);

        int count = Integer.parseInt(countOverride);

        if (count < 10) {
            scaledX -= 5;
        } else if (count < 100) {
            scaledX -= 2;
        }

        drawContext.drawStackOverlay(mc.textRenderer, itemStack, scaledX, scaledY + 8, countOverride);

        matrices.pop();
    }

}
