package com.genyo.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Render3DEngine {

    private static boolean registered = false;

    public static final List<LineAction> LINE_QUEUE = new ArrayList<>();

    public Render3DEngine() {
        if (registered) {
            throw new IllegalStateException(
                "Render3DEngine was constructed/registered more than once. "
            );
        }
        registered = true;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        for (LineAction action : LINE_QUEUE) {
            event.renderer.line(
                action.start.x, action.start.y, action.start.z,
                action.end.x, action.end.y, action.end.z,
                action.color
            );
        }
        LINE_QUEUE.clear();
    }

    public static void drawLine(Vec3d start, Vec3d end, Color color) {
        LINE_QUEUE.add(new LineAction(start, end, color));
    }

    public record LineAction(Vec3d start, Vec3d end, Color color) {}
}
