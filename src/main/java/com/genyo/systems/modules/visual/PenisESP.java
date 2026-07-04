package com.genyo.systems.modules.visual;

import com.genyo.Genyo;
import com.genyo.managers.Managers;
import com.genyo.systems.modules.GenyoModule;
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

import java.util.ArrayList;
import java.util.List;

public class PenisESP extends GenyoModule {

    public PenisESP() {
        super(Genyo.VISUAL, "PenisESP", "faszfasz fasz fasz fasz fasz fsaz fasz");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("Only Own")
        .description("ya")
        .defaultValue(false)
        .build()
    );

    private final Setting<Float> ballSize = sgGeneral.add(new FloatSetting.Builder()
        .name("Ball Size")
        .description("ya")
        .min(0.1f)
        .defaultValue(0.5f)
        .max(0.5f)
        .build()
    );

    private final Setting<Float> penisSize = sgGeneral.add(new FloatSetting.Builder()
        .name("Penis Size")
        .description("ya")
        .min(0.1f)
        .defaultValue(1.5f)
        .max(3.0f)
        .build()
    );

    private final Setting<Float> friendSize = sgGeneral.add(new FloatSetting.Builder()
        .name("Friend Size")
        .description("fren")
        .min(0.1f)
        .defaultValue(1.5f)
        .max(3.0f)
        .build()
    );

    private final Setting<Float> enemySize = sgGeneral.add(new FloatSetting.Builder()
        .name("Enemy Size")
        .description("emeny >:(")
        .min(0.1f)
        .defaultValue(0.5f)
        .max(3.0f)
        .build()
    );

    private final Setting<Integer> gradation = sgGeneral.add(new IntSetting.Builder()
        .name("Gradation")
        .description("welcome to graduation, good morning.")
        .min(20)
        .defaultValue(30)
        .max(100)
        .build()
    );

    private final Setting<SettingColor> penisColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Penis Color")
        .description("wtf")
        .defaultValue(new Color(231, 180, 122, 255))
        .build()
    );

    private final Setting<SettingColor> headColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Head Color")
        .description("wtf 2.0")
        .defaultValue(new Color(240, 50, 180, 255))
        .build()
    );

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (onlyOwn.get() && player != mc.player) continue;
            double size = (Friends.get().isFriend(player) ? friendSize.get() : (player != mc.player ? enemySize.get() : penisSize.get()));

            Vec3d base = getBase(player, event.tickDelta);

            // Basis vectors relative to the player's facing direction.
            // forwardDir/rightDir are horizontal (yaw only), upDir is world-up.
            // Since forwardDir and rightDir are both horizontal and perpendicular
            // to each other, and upDir is vertical, these three form a clean
            // orthonormal basis - this is what makes ring generation below
            // work correctly regardless of which way the player is facing.
            Vec3d forwardDir = Vec3d.fromPolar(0, player.getYaw());
            Vec3d rightDir = Vec3d.fromPolar(0, player.getYaw() + 90);
            Vec3d upDir = new Vec3d(0, 1, 0);

            // FIX: single offset now (previously this same 0.1 offset was
            // applied again inside drawPenis, pushing the tube start out
            // to 0.2 instead of 0.1).
            Vec3d forward = base.add(0, player.getHeight() / 2.4, 0).add(forwardDir.multiply(0.1));

            Vec3d left = forward.add(rightDir.multiply(-ballSize.get()));
            Vec3d right = forward.add(rightDir.multiply(ballSize.get()));

            drawBall(ballSize.get(), gradation.get(), left, penisColor.get());
            drawBall(ballSize.get(), gradation.get(), right, penisColor.get());
            drawPenis(size, forward, forwardDir, rightDir, upDir);
        }

    }

    public Vec3d getBase(Entity entity, float tickDelta) {
        double x = entity.lastX + ((entity.getX() - entity.lastX) * tickDelta);
        double y = entity.lastY + ((entity.getY() - entity.lastY) * tickDelta);
        double z = entity.lastZ + ((entity.getZ() - entity.lastZ) * tickDelta);

        return new Vec3d(x, y, z);
    }

    public void drawBall(double radius, int gradation, Vec3d pos, Color color) {
        float alpha, beta;

        for (alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / gradation) {
            for (beta = 0.0f; beta < 2.0 * Math.PI; beta += Math.PI / gradation) {
                double x1 = pos.getX() + (radius * Math.cos(beta) * Math.sin(alpha));
                double y1 = pos.getY() + (radius * Math.sin(beta) * Math.sin(alpha));
                double z1 = pos.getZ() + (radius * Math.cos(alpha));

                double sinNext = Math.sin(alpha + Math.PI / gradation);
                double x2 = pos.getX() + (radius * Math.cos(beta) * sinNext);
                double y2 = pos.getY() + (radius * Math.sin(beta) * sinNext);
                double z2 = pos.getZ() + (radius * Math.cos(alpha + Math.PI / gradation));

                Managers.ENGINE3D.drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), color);
            }
        }
    }


    public void drawPenis(double size, Vec3d start, Vec3d forwardDir, Vec3d rightDir, Vec3d upDir) {
        double tubeRadius = 0.1;
        Vec3d end = start.add(forwardDir.multiply(size));

        drawTube(start, end, tubeRadius, gradation.get(), rightDir, upDir, penisColor.get());

        drawBall(tubeRadius, gradation.get(), start, penisColor.get());
        drawBall(tubeRadius, gradation.get(), end, headColor.get());
    }

    public void drawTube(Vec3d start, Vec3d end, double radius, int segments, Vec3d rightDir, Vec3d upDir, Color color) {
        List<Vec3d> startRing = new ArrayList<>();
        List<Vec3d> endRing = new ArrayList<>();

        for (int i = 0; i < segments; i++) {
            double theta = (2 * Math.PI * i) / segments;
            Vec3d offset = rightDir.multiply(radius * Math.cos(theta)).add(upDir.multiply(radius * Math.sin(theta)));
            startRing.add(start.add(offset));
            endRing.add(end.add(offset));
        }

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            Managers.ENGINE3D.drawLine(startRing.get(i), startRing.get(next), color); // start ring
            Managers.ENGINE3D.drawLine(endRing.get(i), endRing.get(next), color);     // end ring
            Managers.ENGINE3D.drawLine(startRing.get(i), endRing.get(i), color);       // side "rung"
        }
    }


}
