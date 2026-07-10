package com.genyo.systems.modules.misc;


import baritone.api.BaritoneAPI;
import baritone.api.selection.ISelection;
import com.genyo.Genyo;
import com.genyo.systems.modules.GenyoModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GenyoNuker extends GenyoModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How the nuker determines which area to mine.")
        .defaultValue(Mode.Normal)
        .build()
    );

    public final Setting<Boolean> ignoreWhitelist = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-whitelist")
        .description("Whether or not to ignore the whitelist.")
        .defaultValue(true)
        .build()
    );

    public final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("A list of blocks to mine.")
        .defaultValue(List.of(Blocks.EMERALD_BLOCK, Blocks.END_STONE))
        .build()
    );

    public final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Max mining distance.")
        .defaultValue(3.75)
        .range(2.0, 5.0)
        .sliderRange(2.0, 5.0)
        .build()
    );

    public final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y")
        .defaultValue(5)
        .range(0, 5)
        .sliderRange(0, 5)
        .build()
    );

    public final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("min-y")
        .defaultValue(0)
        .range(-5, 0)
        .sliderRange(-5, 0)
        .build()
    );

    public final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to mine per tick.")
        .defaultValue(12)
        .range(1, 32)
        .sliderRange(1, 32)
        .build()
    );

    public final Setting<Boolean> doubleMine = sgGeneral.add(new BoolSetting.Builder()
        .name("double-mine")
        .description("Attempts to double mine when possible, slower for terrain.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> radius = sgRender.add(new BoolSetting.Builder()
        .name("radius")
        .description("Whether or not to render a sphere the size of your reach. (Requires custom renderer)")
        .defaultValue(false)
        .build()
    );

    public final Setting<SettingColor> radiusColor = sgRender.add(new ColorSetting.Builder()
        .name("radius-color")
        .defaultValue(new SettingColor(0, 0, 255, 38))
        .build()
    );

    public final Setting<Boolean> attemptBreak = sgRender.add(new BoolSetting.Builder()
        .name("attempt-break")
        .defaultValue(true)
        .build()
    );

    public final Setting<SettingColor> breakColor = sgRender.add(new ColorSetting.Builder()
        .name("break-color")
        .defaultValue(new SettingColor(0, 255, 0, 102))
        .build()
    );

    public final Setting<Integer> expireTime = sgRender.add(new IntSetting.Builder()
        .name("expire-time")
        .defaultValue(2500)
        .range(1000, 10000)
        .sliderRange(1000, 10000)
        .build()
    );

    public BlockPos[] doubleMineBlocks = new BlockPos[2];
    public List<BrokenBlock> brokenBlocks = new ArrayList<>();

    public GenyoNuker() {
        super(Genyo.WORLD, "genyo-nuker", "Very fast block breaking brrr.");
    }
    @Override
    public void onActivate() {
        doubleMineBlocks[0] = null;
        doubleMineBlocks[1] = null;
        brokenBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        List<BlockPos> blockVecs = new ArrayList<>();

        int radiusInt = 7;
        BlockPos playerPos = mc.player.getBlockPos();

        for (int y = minY.get(); y <= maxY.get(); y++) {
            BlockPos center = playerPos.add(0, y, 0);

            for (int x = -radiusInt; x <= radiusInt; x++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    mutableBlockPos.set(center.getX() + x, center.getY(), center.getZ() + z);

                    // 2. Baritone Selection ellenőrzés
                    if (mode.get() == Mode.BaritoneSelection && !isInBaritoneSelection(mutableBlockPos)) {
                        continue;
                    }

                    if (!ignoreWhitelist.get() && !blocks.get().contains(mc.world.getBlockState(mutableBlockPos).getBlock())) {
                        continue;
                    }

                    BlockState state = mc.world.getBlockState(mutableBlockPos);

                    if (state.getHardness(mc.world, mutableBlockPos) == -1.0f) {
                        continue;
                    }

                    if (!mc.world.getFluidState(mutableBlockPos).isEmpty()) {
                        continue;
                    }

                    if (state.isAir()) {
                        continue;
                    }

                    if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(mutableBlockPos)) <= maxDistance.get()) {
                        blockVecs.add(mutableBlockPos.toImmutable());
                    }
                }
            }
        }

        blockVecs.sort(Comparator.comparingDouble((pos) ->
            Math.abs(Rotations.getYaw(pos) - mc.player.getYaw()) + Math.abs(Rotations.getPitch(pos) - mc.player.getPitch())));

        if (doubleMine.get()) {
            boolean waitingForMineFinish = false;
            if (doubleMineBlocks[0] != null) {
                if (mc.world.getBlockState(doubleMineBlocks[0]).isAir()) {
                    doubleMineBlocks[0] = null;
                } else {
                    waitingForMineFinish = true;
                }
            }

            if (doubleMineBlocks[1] != null) {
                if (mc.world.getBlockState(doubleMineBlocks[1]).isAir()) {
                    doubleMineBlocks[1] = null;
                } else {
                    waitingForMineFinish = true;
                }
            }

            if (!waitingForMineFinish) {
                if (blockVecs.size() >= 2) {
                    processDoubleMineBlock(blockVecs.get(0), 0);
                    processDoubleMineBlock(blockVecs.get(1), 1);
                } else if (blockVecs.size() == 1) {
                    processDoubleMineBlock(blockVecs.get(0), 0);
                    doubleMineBlocks[1] = null;
                }
            }
            return;
        }

        for (int i = 0; i < blocksPerTick.get(); i++) {
            if (blockVecs.size() <= i) break;

            BlockPos pos = blockVecs.get(i);
            mutableBlockPos.set(pos.getX(), pos.getY(), pos.getZ());

            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(mutableBlockPos)) > maxDistance.get()) continue;

            // Meteor AutoTool Logic
            FindItemResult bestTool = InvUtils.findFastestTool(mc.world.getBlockState(mutableBlockPos));
            if (bestTool.found()) {
                InvUtils.swap(bestTool.slot(), false);
            }

            brokenBlocks.removeIf(brokenBlock -> brokenBlock.pos.equals(mutableBlockPos));
            brokenBlocks.add(new BrokenBlock(mutableBlockPos.toImmutable(), System.currentTimeMillis(), expireTime.get()));

            sendMinePackets(mutableBlockPos);
        }

        brokenBlocks.removeIf(BrokenBlock::isExpired);
    }

    private void processDoubleMineBlock(BlockPos targetPos, int index) {
        BlockPos.Mutable mutableBlockPos = targetPos.mutableCopy();

        FindItemResult bestTool = InvUtils.findFastestTool(mc.world.getBlockState(mutableBlockPos));
        if (bestTool.found()) {
            InvUtils.swap(bestTool.slot(), false);
        }

        sendMinePackets(mutableBlockPos);
        doubleMineBlocks[index] = mutableBlockPos.toImmutable();
    }

    private void sendMinePackets(BlockPos.Mutable mutableBlockPos) {
        int originalY = mutableBlockPos.getY();
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mutableBlockPos, getDirection(mutableBlockPos)));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, mutableBlockPos.setY(originalY + 1337), getDirection(mutableBlockPos.setY(originalY))));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mutableBlockPos.setY(originalY), getDirection(mutableBlockPos)));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (breakColor.get().a > 0 && attemptBreak.get()) {
            for (BrokenBlock block : brokenBlocks) {
                if (block.isExpired()) continue;

                // Alpha fade calculation
                int alpha = (int) Math.min(255, breakColor.get().a * block.getAlpha());
                SettingColor fadedSide = new SettingColor(breakColor.get().r, breakColor.get().g, breakColor.get().b, alpha);
                SettingColor fadedLine = new SettingColor(breakColor.get().r, breakColor.get().g, breakColor.get().b, 255);

                event.renderer.box(block.pos, fadedSide, fadedLine, ShapeMode.Both, 0);
            }
        }
    }

    private boolean isInBaritoneSelection(BlockPos pos) {
        ISelection[] selections = BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().getSelections();
        if (selections.length == 0) return false;

        for (ISelection selection : selections) {
            BlockPos min = selection.min();
            BlockPos max = selection.max();

            if (pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                return true;
            }
        }
        return false;
    }

    public Direction getDirection(BlockPos pos) {
        double eyePos = mc.player.getEyeY();
        VoxelShape outline = mc.world.getBlockState(pos).getCollisionShape(mc.world, pos, ShapeContext.of(mc.player));

        if (outline.isEmpty()) return Direction.UP;

        if (eyePos > pos.getY() + outline.getMax(Direction.Axis.Y) && mc.world.getBlockState(pos.up()).isReplaceable()) {
            return Direction.UP;
        } else if (eyePos < pos.getY() + outline.getMin(Direction.Axis.Y) && mc.world.getBlockState(pos.down()).isReplaceable()) {
            return Direction.DOWN;
        } else {
            BlockPos difference = pos.subtract(mc.player.getBlockPos());

            if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
                return difference.getX() > 0 ? Direction.WEST : Direction.EAST;
            } else {
                return difference.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
            }
        }
    }

    public static class BrokenBlock {
        public final BlockPos pos;
        public final long keepAliveMS;
        public final long expireTimeMS;

        public BrokenBlock(BlockPos pos, long keepAliveMS, long expireTimeMS) {
            this.pos = pos;
            this.keepAliveMS = keepAliveMS;
            this.expireTimeMS = expireTimeMS;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - keepAliveMS >= expireTimeMS;
        }

        public float getAlpha() {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - keepAliveMS;

            if (elapsedTime >= expireTimeMS || expireTimeMS <= 0) {
                return 0.0f;
            }

            float remainingTime = expireTimeMS - elapsedTime;
            return Math.max(0.0f, remainingTime / (float) expireTimeMS);
        }
    }

    public enum Mode {
        Normal,
        BaritoneSelection
    }
}
