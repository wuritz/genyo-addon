package com.genyo.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class PlayerWireframeRenderer {

    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0}, // bottom face
        {4, 5}, {5, 6}, {6, 7}, {7, 4}, // top face
        {0, 4}, {1, 5}, {2, 6}, {3, 7}  // verticals connecting bottom to top
    };

    public static void render(Render3DEvent event, ModelPart root,
                              double originX, double originY, double originZ,
                              float bodyYaw, Color color) {
        MatrixStack matrices = new MatrixStack();

        // Mirrors LivingEntityRenderer.render(): body-yaw rotation, then the
        // model-space -> world-space flip and vertical offset used for every
        // biped model render.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.501F, 0.0F);

        root.forEachCuboid(matrices, (entry, path, index, cuboid) -> {
            Matrix4f mat = entry.getPositionMatrix();

            Vector3f[] corners = new Vector3f[] {
                new Vector3f(cuboid.minX / 16f, cuboid.minY / 16f, cuboid.minZ / 16f),
                new Vector3f(cuboid.maxX / 16f, cuboid.minY / 16f, cuboid.minZ / 16f),
                new Vector3f(cuboid.maxX / 16f, cuboid.minY / 16f, cuboid.maxZ / 16f),
                new Vector3f(cuboid.minX / 16f, cuboid.minY / 16f, cuboid.maxZ / 16f),
                new Vector3f(cuboid.minX / 16f, cuboid.maxY / 16f, cuboid.minZ / 16f),
                new Vector3f(cuboid.maxX / 16f, cuboid.maxY / 16f, cuboid.minZ / 16f),
                new Vector3f(cuboid.maxX / 16f, cuboid.maxY / 16f, cuboid.maxZ / 16f),
                new Vector3f(cuboid.minX / 16f, cuboid.maxY / 16f, cuboid.maxZ / 16f),
            };

            for (Vector3f corner : corners) {
                mat.transformPosition(corner);
            }

            for (int[] edge : EDGES) {
                Vector3f a = corners[edge[0]];
                Vector3f b = corners[edge[1]];

                event.renderer.line(
                    originX + a.x, originY + a.y, originZ + a.z,
                    originX + b.x, originY + b.y, originZ + b.z,
                    color
                );
            }
        });
    }
}
