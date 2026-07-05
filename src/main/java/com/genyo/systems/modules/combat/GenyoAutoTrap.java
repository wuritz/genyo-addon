package com.genyo.systems.modules.combat;

import com.genyo.Genyo;
import com.genyo.events.network.PlayerTickEvent;
import com.genyo.managers.Managers;
import com.genyo.systems.modules.PlacerModule;
import com.genyo.render.animation.Animation;
import com.genyo.systems.settings.FloatSetting;
import com.genyo.utils.math.GPositionUtils;
import com.genyo.utils.math.MathUtil;
import com.genyo.utils.world.BlastResistantBlocks;
import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AirPlace;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class GenyoAutoTrap extends PlacerModule {

    public GenyoAutoTrap() {
        super(Genyo.COMBAT, "genyo-auto-trap", "Fully traps enemies with blocks.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Float> placeRangeConfig = sgGeneral.add(new FloatSetting.Builder()
        .name("Place Range")
        .description("The placement range for trap")
        .min(0.0f)
        .defaultValue(4.0f)
        .max(6.0f)
        .build()
    );

    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("Rotates to block before placing")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> attackConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack")
        .description("Attacks crystals in the way of trap")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> extendConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Extend")
        .description("Extends trap if the player is not in the center of a block")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> supportConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Support")
        .description("Creates a floor for the trap if there is none")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> headConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Head")
        .description("Place a block at targets head")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiStepConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Prevent Step")
        .description("Prevents target from stepping out of the trap")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> shiftTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Shift Ticks")
        .description("The number of blocks to place per tick")
        .min(1)
        .defaultValue(2)
        .max(10)
        .build()
    );

    private final Setting<Float> shiftDelayConfig = sgGeneral.add(new FloatSetting.Builder()
        .name("Shift Delay")
        .description("The delay between each block placement interval")
        .min(0.0f)
        .defaultValue(1.0f)
        .max(5.0f)
        .build()
    );

    private final Setting<Boolean> autoDisableConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Disable")
        .description("Disables after placing the blocks")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render")
        .description("Renders where trap is placing blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("Render Color")
        .description("asdsadsadsadsadsa")
        .defaultValue(new Color(236, 243, 122, 40))
        .build()
    );

    private final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time")
        .description("Time to fade")
        .min(0)
        .defaultValue(250)
        .max(1000)
        .visible(() -> false)
        .build()
    );

    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private int blocksPlaced;

    @Override
    public void onDeactivate()
    {
        surround.clear();
        placements.clear();
        packets.clear();
        fadeList.clear();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event)
    {
        blocksPlaced = 0;

        if (!multitask.get() && mc.player.isUsingItem())
        {
            surround.clear();
            placements.clear();
            return;
        }

        final int slot = getResistantBlockItem();
        if (slot == -1)
        {
            surround.clear();
            placements.clear();
            return;
        }
        PlayerEntity trapTarget = getTrapTarget();
        if (trapTarget == null)
        {
            surround.clear();
            placements.clear();
            return;
        }

        BlockPos targetBlockPos = GPositionUtils.getRoundedBlockPos(trapTarget.getX(), trapTarget.getY(), trapTarget.getZ());
        surround = getSurround(targetBlockPos, trapTarget);
        if (surround.isEmpty())
        {
            return;
        }
        if (attackConfig.get())
        {
            attackBlockingCrystals(surround);
        }
        placements = getPlacementsFromSurround(surround);
        if (placements.isEmpty())
        {
            if (autoDisableConfig.get())
            {
                toggle();
                sendDisableMsg("No placements found.");
            }
            return;
        }
        if (supportConfig.get())
        {
            List<BlockPos> supportBlocks = new ArrayList<>();
            for (BlockPos block : new ArrayList<>(placements))
            {
                if (isAirPlace(block) && mc.world.getBlockState(block).isReplaceable())
                {
                    BlockPos below = block.down();
                    if (!placements.contains(below) && !supportBlocks.contains(below)
                        && mc.world.getBlockState(below).isReplaceable())
                    {
                        supportBlocks.add(below);
                    }
                }
            }
            placements.addAll(0, supportBlocks);
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));
        while (blocksPlaced < shiftTicksConfig.get())
        {
            if (blocksPlaced >= placements.size())
            {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            blocksPlaced++;
            // All rotations for shift ticks must send extra packet
            // This may not work on all servers
            placeBlock(targetPos, slot);
        }

        if (rotateConfig.get())
        {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }
        if (event.packet instanceof BundleS2CPacket packet)
        {
            for (Packet<?> packet1 : packet.getPackets())
            {
                handlePackets(packet1);
            }
        }
        else
        {
            handlePackets(event.packet);
        }
    }

    private void handlePackets(Packet<?> serverPacket)
    {
        if (serverPacket instanceof BlockUpdateS2CPacket packet)
        {
            final BlockState blockState = packet.getState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos))
            {
                if (blockState.isReplaceable() && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent()))
                {
                    final int slot = getResistantBlockItem();
                    if (slot == -1)
                    {
                        return;
                    }
                    placeBlock(targetPos, slot);
                }
                else if (BlastResistantBlocks.isBlastResistant(blockState))
                {
                    packets.remove(targetPos);
                }
            }
        }
    }

    private void placeBlock(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirection.get(), false, true, (state, angles) ->
        {
            if (rotateConfig.get() && state)
            {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
    }

    private PlayerEntity getTrapTarget()
    {
        final List<Entity> entities = Lists.newArrayList(mc.world.getEntities());
        return (PlayerEntity) entities.stream()
            .filter(e -> e instanceof PlayerEntity player && e.isAlive() && mc.player != e && !Managers.SOCIAL.isFriend(player))
            .filter(e -> mc.player.squaredDistanceTo(e) <= MathUtil.squared(placeRangeConfig.get()))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
    }

    public void attackBlockingCrystals(List<BlockPos> posList)
    {
        for (BlockPos pos : posList)
        {
            Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream()
                .filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
            if (crystalEntity == null)
            {
                continue;
            }
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
        }
    }

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround)
    {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround)
        {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.get() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.get() * 50.0f)
            {
                continue;
            }
            if (!mc.world.getBlockState(surroundPos).isReplaceable())
            {
                continue;
            }
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (dist > MathUtil.squared(placeRangeConfig.get()))
            {
                continue;
            }

            if (mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, surroundPos, ShapeContext.absent()))
            {
                placements.add(surroundPos);
            }
        }
        return placements;
    }

    public List<BlockPos> getSurround(BlockPos playerPos, PlayerEntity player)
    {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(playerPos, player);
        for (BlockPos pos : playerBlocks)
        {
            for (Direction dir : Direction.values())
            {
                if (!dir.getAxis().isHorizontal())
                {
                    continue;
                }
                BlockPos pos1 = pos.offset(dir);
                if (surroundBlocks.contains(pos1) || playerBlocks.contains(pos1))
                {
                    continue;
                }

                surroundBlocks.add(pos1);
                surroundBlocks.add(pos1.up());
            }
        }
        if (headConfig.get())
        {
            boolean support = false;
            final List<BlockPos> headBlocks = new ArrayList<>();
            for (BlockPos pos : playerBlocks)
            {
                BlockPos headPos = pos.offset(Direction.UP, 2);
                if (!mc.world.getBlockState(headPos).isReplaceable())
                {
                    support = true;
                }
                headBlocks.add(headPos);
                if (antiStepConfig.get())
                {
                    BlockPos antiStepPos = pos.offset(Direction.UP, 3);
                    headBlocks.add(antiStepPos);
                }
            }
            if (!Modules.get().isActive(AirPlace.class))
            {
                BlockPos supportingPos = null;
                double min = Double.MAX_VALUE;
                for (BlockPos pos : surroundBlocks)
                {
                    BlockPos pos1 = pos.offset(Direction.UP, 2);
                    if (!mc.world.getBlockState(pos1).isReplaceable())
                    {
                        support = true;
                        break;
                    }
                    double dist = mc.player.squaredDistanceTo(pos1.toCenterPos());
                    if (dist < min)
                    {
                        supportingPos = pos1;
                        min = dist;
                    }
                }
                if (supportingPos != null && !support)
                {
                    surroundBlocks.add(supportingPos);
                }
            }
            surroundBlocks.addAll(headBlocks);
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(BlockPos playerPos, PlayerEntity entity)
    {
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extendConfig.get())
        {
            playerBlocks.addAll(GPositionUtils.getAllInBox(entity.getBoundingBox(), playerPos));
        }
        else
        {
            playerBlocks.add(playerPos);
        }
        return playerBlocks;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event)
    {
        if (renderConfig.get())
        {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());

                Color boxColor = color.get().a(boxAlpha);
                Color lineColor = color.get().a(lineAlpha);

                event.renderer.box(set.getKey(), boxColor, lineColor, ShapeMode.Both, 0);
            }

            if (placements.isEmpty())
            {
                return;
            }

            for (BlockPos pos : placements)
            {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }
    private boolean isAirPlace(BlockPos blockPos)
    {
        for (Direction dir : Direction.values())
        {
            if (!mc.world.getBlockState(blockPos.offset(dir)).isReplaceable()) return false;
        }
        return true;
    }
    public boolean isPlacing()
    {
        return !placements.isEmpty();
    }

}
