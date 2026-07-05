package com.genyo.systems.modules.visual;

import com.genyo.Genyo;
import com.genyo.systems.modules.GenyoModule;
import com.genyo.render.Render3DEngine;
import com.genyo.systems.settings.FloatSetting;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class GyattESP extends GenyoModule {

    public GyattESP() {
        super(Genyo.VISUAL, "GyattESP", "ya");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("Only Own")
        .description("ya")
        .defaultValue(false)
        .build()
    );

    private final Setting<Float> cheekSize = sgGeneral.add(new FloatSetting.Builder()
        .name("Cheek Size")
        .description("radius of each sphere")
        .min(0.1f)
        .defaultValue(0.6f)
        .max(2.0f)
        .build()
    );

    private final Setting<Float> gap = sgGeneral.add(new FloatSetting.Builder()
        .name("Gap")
        .description("distance between the two centers - set below cheek size for overlap")
        .min(0.0f)
        .defaultValue(0.5f)
        .max(2.0f)
        .build()
    );

    private final Setting<Float> friendSize = sgGeneral.add(new FloatSetting.Builder()
        .name("Friend Size")
        .description("fren")
        .min(0.1f)
        .defaultValue(0.6f)
        .max(2.0f)
        .build()
    );

    private final Setting<Float> enemySize = sgGeneral.add(new FloatSetting.Builder()
        .name("Enemy Size")
        .description("emeny >:(")
        .min(0.1f)
        .defaultValue(0.3f)
        .max(2.0f)
        .build()
    );

    private final Setting<Integer> gradation = sgGeneral.add(new IntSetting.Builder()
        .name("Gradation")
        .description("welcome to graduation, good morning.")
        .min(20)
        .defaultValue(30)
        .max(100)
        .sliderRange(20, 100)
        .build()
    );

    private final Setting<SettingColor> cheekColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("wtf")
        .defaultValue(new Color(231, 180, 122, 255))
        .build()
    );

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (onlyOwn.get() && player != mc.player) continue;

            double size = Friends.get().isFriend(player)
                ? friendSize.get()
                : (player != mc.player ? enemySize.get() : cheekSize.get());

            Vec3d base = getInterpolatedPos(player, event.tickDelta);

            Vec3d forwardDir = Vec3d.fromPolar(0, player.getYaw());
            Vec3d rightDir = Vec3d.fromPolar(0, player.getYaw() + 90);

            Vec3d hip = base.add(0, player.getHeight() / 2.4, 0)
                .add(forwardDir.multiply(-0.15));

            double offset = (size + gap.get()) / 2.0;

            Vec3d left = hip.add(rightDir.multiply(-offset));
            Vec3d right = hip.add(rightDir.multiply(offset));

            drawSphere(size, gradation.get(), left, cheekColor.get());
            drawSphere(size, gradation.get(), right, cheekColor.get());
        }
    }

    private Vec3d getInterpolatedPos(Entity entity, float tickDelta) {
        double x = entity.lastX + ((entity.getX() - entity.lastX) * tickDelta);
        double y = entity.lastY + ((entity.getY() - entity.lastY) * tickDelta);
        double z = entity.lastZ + ((entity.getZ() - entity.lastZ) * tickDelta);
        return new Vec3d(x, y, z);
    }

    private void drawSphere(double radius, int gradation, Vec3d pos, Color color) {
        for (float alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / gradation) {
            for (float beta = 0.0f; beta < 2.0 * Math.PI; beta += Math.PI / gradation) {
                double x1 = pos.getX() + (radius * Math.cos(beta) * Math.sin(alpha));
                double y1 = pos.getY() + (radius * Math.sin(beta) * Math.sin(alpha));
                double z1 = pos.getZ() + (radius * Math.cos(alpha));

                double sinNext = Math.sin(alpha + Math.PI / gradation);
                double x2 = pos.getX() + (radius * Math.cos(beta) * sinNext);
                double y2 = pos.getY() + (radius * Math.sin(beta) * sinNext);
                double z2 = pos.getZ() + (radius * Math.cos(alpha + Math.PI / gradation));

                Render3DEngine.drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), color);
            }
        }
    }
}
