package com.genyo.systems.modules.combat;


import com.genyo.Genyo;
import com.genyo.systems.modules.GenyoModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MineESP extends GenyoModule {

    public enum RenderMode {
        Scale, Fill, Liquid
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("At what distance should mined blocks be displayed?.")
        .defaultValue(30)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> syncDoubleMine = sgGeneral.add(new BoolSetting.Builder()
        .name("sync-double-mine")
        .description("Try to guess and synchronize the second mined block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("How the mining animation should appear.")
        .defaultValue(RenderMode.Liquid)
        .build()
    );

    private final Setting<Boolean> showOwnMining = sgGeneral.add(new BoolSetting.Builder()
        .name("show-own-mining")
        .description("Az ESP a saját bányászásunkat is mutassa.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Using Meteor's unique typeface.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<SettingColor> rebreakSideColor = sgRender.add(new ColorSetting.Builder()
        .name("rebreak-side-color")
        .description("The color of the box if a rebreak occurs.")
        .defaultValue(new SettingColor(200, 50, 255, 100))
        .build()
    );

    private final Setting<SettingColor> rebreakLineColor = sgRender.add(new ColorSetting.Builder()
        .name("rebreak-line-color")
        .defaultValue(new SettingColor(200, 50, 255, 255))
        .build()
    );

    private final Setting<SettingColor> doubleMineSideColor = sgColors.add(new ColorSetting.Builder()
        .name("double-mine-side-color")
        .description("A doboz színe, ha a játékos egyszerre több blokkot bányászik.")
        .defaultValue(new SettingColor(255, 165, 0, 100))
        .build()
    );

    private final Setting<SettingColor> doubleMineLineColor = sgColors.add(new ColorSetting.Builder()
        .name("double-mine-line-color")
        .defaultValue(new SettingColor(255, 165, 0, 255))
        .build()
    );

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> sideColorStart = sgRender.add(new ColorSetting.Builder()
        .name("side-color-start")
        .defaultValue(new SettingColor(50, 255, 50, 70))
        .build()
    );

    private final Setting<SettingColor> sideColorEnd = sgRender.add(new ColorSetting.Builder()
        .name("side-color-end")
        .defaultValue(new SettingColor(255, 50, 50, 70))
        .build()
    );

    private final Setting<SettingColor> lineColorStart = sgRender.add(new ColorSetting.Builder()
        .name("line-color-start")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .build()
    );

    private final Setting<SettingColor> lineColorEnd = sgRender.add(new ColorSetting.Builder()
        .name("line-color-end")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Map<BlockPos, MineData> blocks = new ConcurrentHashMap<>();
    private final Map<Integer, BlockPos> lastBrokenBlocks = new ConcurrentHashMap<>();
    private BlockPos selfBreakingPos = null;

    public MineESP() {
        super(Genyo.COMBAT, "mine-esp", "Featuring ESP Liquid effect, with Double Mine and Rebreak synchronization.");
    }

    @Override
    public void onDeactivate() {
        blocks.clear();
        lastBrokenBlocks.clear();
        selfBreakingPos = null;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof BlockBreakingProgressS2CPacket packet) {
            BlockPos pos = packet.getPos();
            int stage = packet.getProgress();
            int entityId = packet.getEntityId();

            if (stage >= 0 && stage < 10) {
                MineData data = blocks.get(pos);
                if (data == null) {
                    boolean isRebreak = pos.equals(lastBrokenBlocks.get(entityId));
                    blocks.put(pos, new MineData(pos, entityId, stage, isRebreak));
                } else {
                    data.targetStage = stage;
                    data.timer = 0;
                }
            } else {
                lastBrokenBlocks.put(entityId, pos);
                blocks.remove(pos);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        updateOwnMining();

        blocks.entrySet().removeIf(entry -> {
            MineData data = entry.getValue();
            data.timer++;
            if (data.timer > 400 || mc.world.getBlockState(data.pos).isAir()) return true;
            double target = (data.targetStage + 1) / 10.0;
            data.renderProgress += (target - data.renderProgress) * 0.15;
            return false;
        });

        if (syncDoubleMine.get()) {
            Map<Integer, Integer> activeCounts = new ConcurrentHashMap<>();
            for (MineData data : blocks.values()) {
                activeCounts.merge(data.entityId, 1, Integer::sum);
            }
            for (MineData data : blocks.values()) {
                data.isDoubleMine = activeCounts.getOrDefault(data.entityId, 0) > 1;
            }
        } else {
            for (MineData data : blocks.values()) {
                data.isDoubleMine = false;
            }
        }
    }

    private void updateOwnMining() {
        if (mc.player == null || mc.interactionManager == null || !showOwnMining.get()) {
            if (selfBreakingPos != null) {
                blocks.remove(selfBreakingPos);
                selfBreakingPos = null;
            }
            return;
        }

        boolean breakingNow = mc.interactionManager.isBreakingBlock() && mc.crosshairTarget instanceof BlockHitResult;

        if (!breakingNow) {
            if (selfBreakingPos != null) {
                // If the block wasn't actually mined (i.e. we aborted), remove it immediately
                // instead of waiting for the timeout. A finished block is already air and gets
                // cleaned up by the removeIf air-check right after this runs.
                if (!mc.world.getBlockState(selfBreakingPos).isAir()) {
                    blocks.remove(selfBreakingPos);
                }
                selfBreakingPos = null;
            }
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        int entityId = mc.player.getId();

        int stage = mc.interactionManager.getBlockBreakingProgress();
        if (stage < 0) {
            stage = 0;
        }

        if (selfBreakingPos != null && !selfBreakingPos.equals(pos)) {
            blocks.remove(selfBreakingPos);
        }

        MineData data = blocks.get(pos);
        if (data == null) {
            boolean isRebreak = pos.equals(lastBrokenBlocks.get(entityId));
            blocks.put(pos, new MineData(pos, entityId, stage, isRebreak));
        } else {
            data.targetStage = stage;
            data.timer = 0;
        }
        selfBreakingPos = pos;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null) return;

        for (MineData data : blocks.values()) {
            renderBox(event, data.pos, data);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        for (MineData data : blocks.values()) {
            renderText(event, data.pos, data);
        }
    }

    private void renderBox(Render3DEvent event, BlockPos pos, MineData data) {
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > radius.get() * radius.get()) return;

        Color currentSide;
        Color currentLine;
        if (data.isRebreak) {
            currentSide = rebreakSideColor.get();
            currentLine = rebreakLineColor.get();
        } else if (data.isDoubleMine) {
            currentSide = doubleMineSideColor.get();
            currentLine = doubleMineLineColor.get();
        } else {
            currentSide = lerpColor(sideColorStart.get(), sideColorEnd.get(), data.renderProgress);
            currentLine = lerpColor(lineColorStart.get(), lineColorEnd.get(), data.renderProgress);
        }

        if (renderMode.get() == RenderMode.Liquid) {
            double time = System.currentTimeMillis() / 200.0;
            double sloshX = Math.sin(time) * 0.04;
            double sloshZ = Math.cos(time * 1.2) * 0.04;
            double waveY = Math.sin(time * 1.5) * 0.05;
            double h = Math.max(0.02, Math.min(1.0, data.renderProgress + waveY));

            event.renderer.box(
                pos.getX() + 0.05 + sloshX, pos.getY() + 0.01, pos.getZ() + 0.05 + sloshZ,
                pos.getX() + 0.95 - sloshX, pos.getY() + h, pos.getZ() + 0.95 - sloshZ,
                currentSide, currentLine, ShapeMode.Both, 0
            );
        } else if (renderMode.get() == RenderMode.Fill) {
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + data.renderProgress, pos.getZ() + 1,
                currentSide, currentLine, ShapeMode.Both, 0
            );
        } else {
            double offset = (1.0 - data.renderProgress) / 2.0;
            event.renderer.box(
                pos.getX() + offset, pos.getY() + offset, pos.getZ() + offset,
                pos.getX() + 1 - offset, pos.getY() + 1 - offset, pos.getZ() + 1 - offset,
                currentSide, currentLine, ShapeMode.Both, 0
            );
        }
    }

    private void renderText(Render2DEvent event, BlockPos pos, MineData data) {
        if (mc.player.squaredDistanceTo(pos.toCenterPos()) > radius.get() * radius.get()) return;

        Entity entity = mc.world.getEntityById(data.entityId);
        String minerName = (entity instanceof PlayerEntity) ? entity.getName().getString() : "Valaki";
        int percent = Math.min(100, Math.max(0, (int) Math.round(data.renderProgress * 100)));
        String text = minerName + " " + percent + "%";

        Vector3d pos3d = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3d pos2d = new Vector3d(pos3d);

        if (NametagUtils.to2D(pos2d, 1.5)) {
            String tag = null;
            Color tagColor = null;
            if (data.isRebreak) {
                tag = "Rebreak";
                tagColor = rebreakLineColor.get();
            } else if (data.isDoubleMine) {
                tag = "Double Mine";
                tagColor = doubleMineLineColor.get();
            }

            if (customFont.get()) {
                NametagUtils.begin(pos3d);
                TextRenderer.get().begin(1.0, false, true);
                TextRenderer.get().render(text, -TextRenderer.get().getWidth(text) / 2.0, 0, textColor.get(), true);
                if (tag != null) {
                    TextRenderer.get().render(tag, -TextRenderer.get().getWidth(tag) / 2.0, 12, tagColor, true);
                }
                TextRenderer.get().end();
                NametagUtils.end();
            } else {
                double scale = mc.getWindow().getScaleFactor();
                int x = (int) (pos2d.x / scale) - (mc.textRenderer.getWidth(text) / 2);
                int y = (int) (pos2d.y / scale);
                event.drawContext.drawTextWithShadow(mc.textRenderer, text, x, y, textColor.get().getPacked());
                if (tag != null) {
                    int tagX = (int) (pos2d.x / scale) - (mc.textRenderer.getWidth(tag) / 2);
                    event.drawContext.drawTextWithShadow(mc.textRenderer, tag, tagX, y + 10, tagColor.getPacked());
                }
            }
        }
    }

    private Color lerpColor(Color c1, Color c2, double delta) {
        int r = (int) (c1.r + (c2.r - c1.r) * delta);
        int g = (int) (c1.g + (c2.g - c1.g) * delta);
        int b = (int) (c1.b + (c2.b - c1.b) * delta);
        int a = (int) (c1.a + (c2.a - c1.a) * delta);
        return new Color(r, g, b, a);
    }

    private static class MineData {
        public BlockPos pos;
        public int entityId;
        public int targetStage;
        public double renderProgress;
        public int timer;
        public boolean isRebreak;
        public boolean isDoubleMine;

        public MineData(BlockPos pos, int entityId, int stage, boolean isRebreak) {
            this.pos = pos;
            this.entityId = entityId;
            this.targetStage = stage;
            this.renderProgress = (stage + 1) / 10.0;
            this.isRebreak = isRebreak;
            this.timer = 0;
        }
    }
}
