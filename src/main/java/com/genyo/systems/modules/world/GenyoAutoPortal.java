package com.genyo.systems.modules.world;


import com.genyo.Genyo;
import com.genyo.systems.modules.GenyoModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class GenyoAutoPortal extends GenyoModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick. (1 = safest for anti-cheats)")
        .defaultValue(1)
        .min(1)
        .sliderMax(5)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks between block placements. (0 = fast, 2 = human-like)")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Forces your camera to look at the blocks being placed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the outline of the portal frame being built.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 50))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 255))
        .visible(render::get)
        .build()
    );

    public GenyoAutoPortal() {
        super(Genyo.MISC, "genyo-auto-portal", "Builds a Nether portal with Delay, Auto-Refill, and Hotbar Cleanup.");
    }

    private final List<BlockPos> frameBlocks = new ArrayList<>();
    private final List<BlockPos> insideBlocks = new ArrayList<>();
    private BlockPos firePos = null;

    private int obsOriginalSlot = -1;
    private int flintOriginalSlot = -1;
    private int delayTimer = 0;

    @Override
    public void onActivate() {
        frameBlocks.clear();
        insideBlocks.clear();
        obsOriginalSlot = -1;
        flintOriginalSlot = -1;
        delayTimer = 0;

        if (mc.player == null) return;

        BlockPos p = mc.player.getBlockPos();
        Direction f = mc.player.getHorizontalFacing();
        Direction right = f.rotateYClockwise();
        Direction left = right.getOpposite();
        BlockPos start = p.offset(f);

        frameBlocks.add(start.offset(left));
        frameBlocks.add(start);
        frameBlocks.add(start.offset(right));
        frameBlocks.add(start.offset(right, 2));

        for (int y = 1; y <= 3; y++) {
            frameBlocks.add(start.offset(left).up(y));
            frameBlocks.add(start.offset(right, 2).up(y));
            insideBlocks.add(start.up(y));
            insideBlocks.add(start.offset(right).up(y));
        }

        frameBlocks.add(start.offset(left).up(4));
        frameBlocks.add(start.up(4));
        frameBlocks.add(start.offset(right).up(4));
        frameBlocks.add(start.offset(right, 2).up(4));

        firePos = start.up();

        if (!hasEnoughSpace()) {
            error("Not enough space! There are blocks in the way.");
            toggle();
            return;
        }

        int missingBlocks = 0;
        for (BlockPos pos : frameBlocks) {
            if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) missingBlocks++;
        }

        int totalObsidian = InvUtils.find(Items.OBSIDIAN).count();
        if (totalObsidian < missingBlocks) {
            error("Not enough obsidian! Required: " + missingBlocks + ", You have: " + totalObsidian);
            toggle();
        }
    }

    private boolean hasEnoughSpace() {
        for (BlockPos pos : frameBlocks) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isReplaceable() && !state.isOf(Blocks.OBSIDIAN)) return false;
        }
        for (BlockPos pos : insideBlocks) {
            if (!mc.world.getBlockState(pos).isReplaceable()) return false;
        }
        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found() && obsOriginalSlot == -1) {
            FindItemResult obsInv = InvUtils.find(Items.OBSIDIAN);
            if (obsInv.found() && !obsInv.isHotbar()) {
                obsOriginalSlot = obsInv.slot();
                InvUtils.move().from(obsOriginalSlot).toHotbar(8);
            }
        }

        obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        boolean allPlaced = true;
        int placedThisTick = 0;

        for (BlockPos pos : frameBlocks) {
            if (mc.world.getBlockState(pos).isReplaceable()) {
                allPlaced = false;

                if (!obsidian.found()) {
                    error("Ran out of obsidian during building!");
                    toggle();
                    return;
                }

                if (BlockUtils.place(pos, obsidian, rotate.get(), 50, true, false)) {
                    placedThisTick++;
                }

                if (placedThisTick >= blocksPerTick.get()) {
                    delayTimer = placeDelay.get();
                    return;
                }
            }
        }

        if (allPlaced && firePos != null) {
            FindItemResult flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);

            if (!flint.found() && flintOriginalSlot == -1) {
                FindItemResult flintInv = InvUtils.find(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);
                if (flintInv.found() && !flintInv.isHotbar()) {
                    flintOriginalSlot = flintInv.slot();
                    InvUtils.move().from(flintOriginalSlot).toHotbar(7);
                }
            }

            flint = InvUtils.findInHotbar(Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);

            if (flint.found()) {
                if (mc.world.getBlockState(firePos).isAir()) {
                    BlockUtils.place(firePos, flint, rotate.get(), 50, true, false);
                }
                info("Portal successfully built and ignited!");
            } else {
                error("Frame is ready, but no flint and steel found in inventory!");
            }
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            if (obsOriginalSlot != -1) {
                InvUtils.move().fromHotbar(8).to(obsOriginalSlot);
                obsOriginalSlot = -1;
            }
            if (flintOriginalSlot != -1) {
                InvUtils.move().fromHotbar(7).to(flintOriginalSlot);
                flintOriginalSlot = -1;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || frameBlocks.isEmpty()) return;
        for (BlockPos pos : frameBlocks) {
            if (!mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
