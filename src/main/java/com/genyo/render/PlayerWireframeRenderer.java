package com.genyo.render;

import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
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

    // 4 corner indices (winding order) per face of the box
    private static final int[][] FACES = {
        {0, 1, 2, 3}, // bottom
        {4, 5, 6, 7}, // top
        {0, 1, 5, 4},
        {1, 2, 6, 5},
        {2, 3, 7, 6},
        {3, 0, 4, 7}
    };

    public static void render(Render3DEvent event, ModelPart root,
                              double originX, double originY, double originZ,
                              float bodyYaw, Color lineColor, Color sideColor, double lineThickness) {
        Renderer3D renderer = event.renderer;
        MatrixStack matrices = new MatrixStack();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.translate(0.0F, -1.501F, 0.0F);

        // Camera position, used to billboard thick-line quads so they face the viewer
        var cam = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();


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

            for (Vector3f corner : corners) mat.transformPosition(corner);

            // Filled side faces
            if (sideColor.a > 0) {
                for (int[] face : FACES) {
                    Vector3f a = corners[face[0]];
                    Vector3f b = corners[face[1]];
                    Vector3f c = corners[face[2]];
                    Vector3f d = corners[face[3]];

                    renderer.quad(
                        originX + a.x, originY + a.y, originZ + a.z,
                        originX + b.x, originY + b.y, originZ + b.z,
                        originX + c.x, originY + c.y, originZ + c.z,
                        originX + d.x, originY + d.y, originZ + d.z,
                        sideColor
                    );
                }
            }

            // Wireframe edges, thickened into billboarded quads
            for (int[] edge : EDGES) {
                Vector3f a = corners[edge[0]];
                Vector3f b = corners[edge[1]];

                drawThickLine(
                    renderer,
                    originX + a.x, originY + a.y, originZ + a.z,
                    originX + b.x, originY + b.y, originZ + b.z,
                    lineColor, lineThickness, cam.x, cam.y, cam.z
                );
            }
        });
    }

    /**
     * Draws a line as a camera-facing quad so it can be given a world-space thickness.
     * Falls back visually to a hairline at very small thickness values.
     */
    private static void drawThickLine(Renderer3D renderer,
                                      double x1, double y1, double z1,
                                      double x2, double y2, double z2,
                                      Color color, double thickness,
                                      double camX, double camY, double camZ) {
        // Direction of the line segment
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        dx /= len; dy /= len; dz /= len;

        // Vector from line midpoint to camera, used to find a perpendicular
        double mx = (x1 + x2) / 2, my = (y1 + y2) / 2, mz = (z1 + z2) / 2;
        double toCamX = camX - mx, toCamY = camY - my, toCamZ = camZ - mz;

        // perpendicular = normalize(dir x toCam)
        double px = dy * toCamZ - dz * toCamY;
        double py = dz * toCamX - dx * toCamZ;
        double pz = dx * toCamY - dy * toCamX;
        double plen = Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 1e-6) return; // line is pointing directly at camera, degenerate
        double half = thickness / 32.0; // thickness setting is in "block/16ths"-ish units; tune to taste
        px = px / plen * half;
        py = py / plen * half;
        pz = pz / plen * half;

        renderer.quad(
            x1 - px, y1 - py, z1 - pz,
            x1 + px, y1 + py, z1 + pz,
            x2 + px, y2 + py, z2 + pz,
            x2 - px, y2 - py, z2 - pz,
            color
        );
    }
}
