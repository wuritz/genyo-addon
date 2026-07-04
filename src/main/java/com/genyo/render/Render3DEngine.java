package com.genyo.render;


import com.genyo.Genyo;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Render3DEngine {

    public static List<FillAction> FILLED_QUEUE = new ArrayList<>();
    public static List<OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
    public static List<FadeAction> FADE_QUEUE = new ArrayList<>();
    public static List<FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
    public static List<OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
    public static List<DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
    public static List<LineAction> LINE_QUEUE = new ArrayList<>();

    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private static float prevCircleStep;
    private static float circleStep;

    // getTickDelta() -> mc.getRenderTickCounter().getTickProgress(true)

    /*@EventHandler
    public void onRender3D(Render3DEvent event) {
        MatrixStack stack = event.matrices;

        if (!FILLED_QUEUE.isEmpty() || !FADE_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            RenderSystem.disableDepthTest();
            setupRender();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, stack.peek().getPositionMatrix(), action.box(), action.color()));

            FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, stack.peek().getPositionMatrix(), action.color(), action.color2()));

            FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, stack.peek().getPositionMatrix(), action.box, action.color(), action.side()));
            Render2DEngine.endBuilding(bufferBuilder);

            endRender();
            RenderSystem.enableDepthTest();

            FADE_QUEUE.clear();
            FILLED_SIDE_QUEUE.clear();
            FILLED_QUEUE.clear();
        }

        if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

            RenderSystem.lineWidth(2f);

            OUTLINE_QUEUE.forEach(action -> {
                setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color());
            });

            OUTLINE_SIDE_QUEUE.forEach(action -> {
                setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side());
            });

            Render2DEngine.endBuilding(buffer);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            OUTLINE_QUEUE.clear();
            OUTLINE_SIDE_QUEUE.clear();
        }

        if (!DEBUG_LINE_QUEUE.isEmpty()) {
            setupRender();
            RenderSystem.disableDepthTest();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.LINES);

            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            DEBUG_LINE_QUEUE.forEach(action -> {
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color);
            });
            Render2DEngine.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            DEBUG_LINE_QUEUE.clear();
        }

        if (!LINE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(2f);
            RenderSystem.disableDepthTest();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            LINE_QUEUE.forEach(action -> {
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color);
            });
            Render2DEngine.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.lineWidth(1f);
            RenderSystem.enableDepthTest();
            endRender();
            LINE_QUEUE.clear();
        }
    }*/

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        FILLED_QUEUE.forEach(action -> event.renderer.boxSides(
            action.box().minX - event.offsetX, action.box().minY - event.offsetY, action.box().minZ - event.offsetZ,
            action.box().maxX - event.offsetX, action.box().maxY - event.offsetY, action.box().maxZ - event.offsetZ,
            action.color(), 0));
        FILLED_QUEUE.clear();

        OUTLINE_QUEUE.forEach(action -> event.renderer.boxLines(
            action.box().minX - event.offsetX, action.box().minY - event.offsetY, action.box().minZ - event.offsetZ,
            action.box().maxX - event.offsetX, action.box().maxY - event.offsetY, action.box().maxZ - event.offsetZ,
            action.color(), 0));
        OUTLINE_QUEUE.clear();

        FILLED_SIDE_QUEUE.forEach(action -> event.renderer.boxSides(
            action.box().minX - event.offsetX, action.box().minY - event.offsetY, action.box().minZ - event.offsetZ,
            action.box().maxX - event.offsetX, action.box().maxY - event.offsetY, action.box().maxZ - event.offsetZ,
            action.color(), 0));
        FILLED_SIDE_QUEUE.clear();

        OUTLINE_SIDE_QUEUE.forEach(action -> event.renderer.boxLines(
            action.box().minX - event.offsetX, action.box().minY - event.offsetY, action.box().minZ - event.offsetZ,
            action.box().maxX - event.offsetX, action.box().maxY - event.offsetY, action.box().maxZ - event.offsetZ,
            action.color(), 0));
        OUTLINE_SIDE_QUEUE.clear();

        LINE_QUEUE.forEach(action -> event.renderer.line(
            action.start().x - event.offsetX, action.start().y - event.offsetY, action.start().z - event.offsetZ,
            action.end().x - event.offsetX, action.end().y - event.offsetY, action.end().z - event.offsetZ,
            action.color()));
        LINE_QUEUE.clear();

        DEBUG_LINE_QUEUE.forEach(action -> event.renderer.line(
            action.start().x - event.offsetX, action.start().y - event.offsetY, action.start().z - event.offsetZ,
            action.end().x - event.offsetX, action.end().y - event.offsetY, action.end().z - event.offsetZ,
            action.color()));
        DEBUG_LINE_QUEUE.clear();

        FADE_QUEUE.clear();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledBox(MatrixStack stack, Box box, Color c) {
        FILLED_QUEUE.add(new FillAction(box, c));
    }

    public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Matrix4f m, @NotNull Box box, @NotNull Color c) {}

    public static @NotNull Box interpolateBox(@NotNull Box from, @NotNull Box to, float delta) {
        double X = Render2DEngine.interpolate(from.maxX, to.maxX, delta);
        double Y = Render2DEngine.interpolate(from.maxY, to.maxY, delta);
        double Z = Render2DEngine.interpolate(from.maxZ, to.maxZ, delta);
        double X1 = Render2DEngine.interpolate(from.minX, to.minX, delta);
        double Y1 = Render2DEngine.interpolate(from.minY, to.minY, delta);
        double Z1 = Render2DEngine.interpolate(from.minZ, to.minZ, delta);
        return new Box(X1, Y1, Z1, X, Y, Z);
    }

    @Deprecated
    public static void drawFilledSide(MatrixStack stack, @NotNull Box box, Color c, Direction dir) {
        FILLED_SIDE_QUEUE.add(new FillSideAction(box, c, dir));
    }

    public static void setFilledSidePoints(BufferBuilder buffer, Matrix4f matrix, Box box, Color c, Direction dir) {}

    public static void drawTextIn3D(String text, @NotNull Vec3d pos, double offX, double offY, double textOffset, @NotNull Color color) {
        MatrixStack matrices = new MatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(pos.getX() - camera.getCameraPos().x, pos.getY() - camera.getCameraPos().y, pos.getZ() - camera.getCameraPos().z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        setupRender();
        matrices.translate(offX, offY - 0.1, -0.01);
        matrices.scale(-0.025f, -0.025f, 0);
        //FontRenderers.sf_medium.drawCenteredString(matrices, text, textOffset, 0f, color.getPacked());
        endRender();
    }

    public static @NotNull Vec3d worldSpaceToScreenSpace(@NotNull Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getCameraPos().x;
        double deltaY = pos.y - camera.getCameraPos().y;
        double deltaZ = pos.z - camera.getCameraPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(target.x / getScaleFactor(), (displayHeight - target.y) / getScaleFactor(), target.z);
    }

    public static double getScaleFactor() {
        //return ClientSettings.scaleFactorFix.getValue() ? ClientSettings.scaleFactorFixValue.getValue() : mc.getWindow().getScaleFactor();
        return mc.getWindow().getScaleFactor();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledFadeBox(@NotNull MatrixStack stack, @NotNull Box box, @NotNull Color c, @NotNull Color c1) {
        FADE_QUEUE.add(new FadeAction(box, c, c1));
    }

    public static void setFilledFadePoints(Box box, BufferBuilder buffer, Matrix4f posMatrix, Color c, Color c1) {}

    public static void drawLine(@NotNull Vec3d start, @NotNull Vec3d end, @NotNull Color color) {
        LINE_QUEUE.add(new LineAction(start, end, color));
    }

    @Deprecated
    public static void drawBoxOutline(@NotNull Box box, Color color, float lineWidth) {
        OUTLINE_QUEUE.add(new OutlineAction(box, color, lineWidth));
    }

    public static void setOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color) {}

    @Deprecated
    public static void drawSideOutline(@NotNull Box box, Color color, float lineWidth, Direction dir) {
        OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(box, color, lineWidth, dir));
    }

    public static void setSideOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color, Direction dir) {}

    public static void drawHoleOutline(@NotNull Box box, Color color, float lineWidth) {}

    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {}

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        matrices.translate(x - camera.getCameraPos().x, y - camera.getCameraPos().y, z - camera.getCameraPos().z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        return matrices;
    }

    public static void setupRender() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endRender() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void drawTargetEsp(MatrixStack stack, @NotNull Entity target) {}

    public static void renderCrosses(@NotNull Box box, Color color, float lineWidth) {}

    public static void drawSphere(MatrixStack matrix, float radius, int slices, int stacks, int color) {}

    public static void drawCylinder(MatrixStack stack, final float radius, final float height, final int slices, final int stacks, int color) {}


    public static void drawCircle3D(MatrixStack stack, Entity ent, float radius, int color, int points, boolean hudColor, int colorOffset) {}

    public static void drawOldTargetEsp(MatrixStack stack, Entity target) {}

    // Kalry не пасть
    // anti yg protection
    public static void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {}

    public static void updateTargetESP() {
        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    public static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    public static Vec3d interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
        double x = prevposX + ((posX - prevposX) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getCameraPos().getX();
        double y = prevposY + ((posY - prevposY) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getCameraPos().getY();
        double z = prevposZ + ((posZ - prevposZ) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ();
        return new Vec3d(x, y, z);
    }

    public static void drawLineDebug(Vec3d start, Vec3d end, Color color) {
        DEBUG_LINE_QUEUE.add(new DebugLineAction(start, end, color));
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickProgress(true);
    }

    public record FillAction(Box box, Color color) {
    }

    public record OutlineAction(Box box, Color color, float lineWidth) {
    }

    public record FadeAction(Box box, Color color, Color color2) {
    }

    public record FillSideAction(Box box, Color color, Direction side) {
    }

    public record OutlineSideAction(Box box, Color color, float lineWidth, Direction side) {
    }

    public record DebugLineAction(Vec3d start, Vec3d end, Color color) {
    }

    public record LineAction(Vec3d start, Vec3d end, Color color) {
    }
}
