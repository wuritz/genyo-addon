package com.genyo.systems.modules.world;

import com.genyo.Genyo;
import com.genyo.managers.Managers;
import com.genyo.systems.modules.PlacerModule;
import com.genyo.render.animation.Animation;
import com.genyo.systems.settings.FloatSetting;
import com.genyo.utils.math.GPositionUtils;
import com.genyo.utils.math.MathUtil;
import com.genyo.utils.player.Rotation;
import com.genyo.utils.world.BlastResistantBlocks;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class GenyoSurroundV2 extends PlacerModule {

    public GenyoSurroundV2() {
        super(Genyo.WORLD, "genyo-surround-v2", "Today I am the shit after I wake the shit is Yes");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("ekwjfkljweklfjewfew")
        .defaultValue(false)
        .build()
    );

    private final Setting<Timing> timing = sgGeneral.add(new EnumSetting.Builder<Timing>()
        .name("Timing")
        .description("Timing for replacing blocks")
        .defaultValue(Timing.VANILLA)
        .build()
    );

    private final Setting<Boolean> prePlaceExplosion = sgGeneral.add(new BoolSetting.Builder()
        .name("Pre Place Explosions")
        .description("Pre places before explosions")
        .defaultValue(false)
        .visible(() -> timing.get() == Timing.SEQUENTIAL)
        .build()
    );

    private final Setting<Boolean> prePlaceTick = sgGeneral.add(new BoolSetting.Builder()
        .name("Pre Place Tick")
        .description("Pre places before ticks")
        .defaultValue(false)
        .visible(() -> timing.get() == Timing.SEQUENTIAL)
        .build()
    );

    private final Setting<Float> placeRange = sgGeneral.add(new FloatSetting.Builder()
        .name("Place Range")
        .description("The placement range for surround")
        .defaultValue(4.0f)
        .min(0.0f)
        .max(6.0f)
        .build()
    );

    private final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack")
        .description("Attacks crystals in the way of surround")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> extend = sgGeneral.add(new BoolSetting.Builder()
        .name("Extend")
        .description("Extends surround if the player is not in the center of a block")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> head = sgGeneral.add(new BoolSetting.Builder()
        .name("Cover Head")
        .description("Place a block at your head")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> mineExtend = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine Extend")
        .description("Extends surround if the block is being mined")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> mineProtect = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine Protect")
        .description("Places a block on surround when an enemy mines past a set threshold")
        .defaultValue(false)
        .build()
    );

    private final Setting<Float> mineProtectThreshold = sgGeneral.add(new FloatSetting.Builder()
        .name("Mine Threshold")
        .description("Mining progress % at which to place a protective block (0-100)")
        .defaultValue(50.0f)
        .min(0.0f)
        .max(100.0f)
        .visible(() -> mineProtect.get())
        .build()
    );

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("Support")
        .description("Creates a floor for the surround if there is none")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> shiftTicks = sgGeneral.add(new IntSetting.Builder()
        .name("Shift Ticks")
        .description("The number of blocks to place per tick")
        .min(1)
        .defaultValue(2)
        .max(10)
        .build()
    );

    private final Setting<Float> shiftDelay = sgGeneral.add(new FloatSetting.Builder()
        .name("Shift Delay")
        .description("The delay between each block placement interval")
        .min(0.0f)
        .defaultValue(1.0f)
        .max(5.0f)
        .build()
    );

    private final Setting<Boolean> jumpDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Disable")
        .description("Disables after moving out of the hole")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("Render")
        .description("Renders where scaffold is placing blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> fadeTime = sgGeneral.add(new IntSetting.Builder()
        .name("Fade Time")
        .description("Time to fade")
        .min(0)
        .defaultValue(250)
        .max(1000)
        .visible(() -> false) // ????
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("Render Color")
        .description("asdsadsadsadsadsa")
        .defaultValue(new Color(236, 243, 122, 40))
        .build()
    );

    private int blocksPlaced;
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private final Map<Integer, long[]> mineProgress = new HashMap<>();
    private final Map<BlockPos, Long> mineProtectCooldown = new HashMap<>();
    private double prevY;

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        prevY = mc.player.getY();
    }

    @Override
    public void onDeactivate() {
        surround.clear();
        placements.clear();
        packets.clear();
        fadeList.clear();
        mineProgress.clear();
        mineProtectCooldown.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        blocksPlaced = 0;

        if (jumpDisable.get() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f)) {
            sendDisableMsg("Player jumped, disabling.");
            toggle();
            return;
        }

        if (!multitask.get() && checkMultitask()) {
            surround.clear();
            placements.clear();
            return;
        }

        final int slot = getResistantBlockItem();
        if (slot == -1) {
            surround.clear();
            placements.clear();
            return;
        }

        surround = getSurround(mc.player);
        if (surround.isEmpty()) return;

        if (attack.get()) attackBlockingCrystals(surround);

        placements = getPlacementsFromSurround(surround);
        if (placements.isEmpty()) return;

        if (support.get()) {
            List<BlockPos> supportBlocks = new ArrayList<>();
            for (BlockPos block : new ArrayList<>(placements)) {
                if (isAirPlace(block) && mc.world.getBlockState(block).isReplaceable()) {
                    BlockPos below = block.down();
                    if (!placements.contains(below) && !supportBlocks.contains(below)
                        && mc.world.getBlockState(below).isReplaceable()) {
                        supportBlocks.add(below);
                    }
                }
            }
            placements.addAll(0, supportBlocks);
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));
        while (blocksPlaced < shiftTicks.get()) {
            if (blocksPlaced >= placements.size())
            {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            // All rotations for shift ticks must send extra packet
            // This may not work on all servers
            placeBlock(targetPos, slot);
        }

        if (rotate.get())
        {
            Rotation.get().setRotationSilentSync();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) handlePackets(packet1);
        } else {
            handlePackets(event.packet);
        }
    }

    private void handlePackets(Packet<?> serverPacket)
    {
        if (serverPacket instanceof BlockBreakingProgressS2CPacket packet && mineProtect.get()) {
            BlockPos minedPos = packet.getPos();
            int entityId = packet.getEntityId();
            int stage = packet.getProgress();

            if (stage < 0) {
                mineProgress.remove(entityId);
                return;
            }

            if (!surround.contains(minedPos)) return;

            float progressPercent = (stage / 9.0f) * 100.0f;
            long[] prev = mineProgress.get(entityId);
            float prevPercent = (prev != null) ? (prev[1] / 9.0f) * 100.0f : 0.0f;

            mineProgress.put(entityId, new long[]{minedPos.asLong(), stage});

            if (prevPercent < mineProtectThreshold.get() && progressPercent >= mineProtectThreshold.get()) {
                Long lastReact = mineProtectCooldown.get(minedPos);
                if (lastReact != null && System.currentTimeMillis() - lastReact < 500) return;

                final int slot = getResistantBlockItem();
                if (slot == -1) return;

                for (BlockPos adjacent : getAdjacentPlacements(minedPos)) {
                    placeBlock(adjacent, slot);
                }
                mineProtectCooldown.put(minedPos, System.currentTimeMillis());
            }
            return;
        }

        if (timing.get() != Timing.SEQUENTIAL) return;

        if (serverPacket instanceof BlockUpdateS2CPacket packet)
        {
            final BlockState blockState = packet.getState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos))
            {
                if (blockState.isReplaceable() && Objects.requireNonNull(mc.world).canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent()))
                {
                    final int slot = getResistantBlockItem();
                    if (slot == -1) return;
                    placeBlock(targetPos, slot);
                }
                else if (BlastResistantBlocks.isBlastResistant(blockState))
                {
                    packets.remove(targetPos);
                }
            }
        }

        if (blocksPlaced > shiftTicks.get() * 2) return;

        if (serverPacket instanceof ExplosionS2CPacket packet && prePlaceExplosion.get())
        {
            BlockPos pos = BlockPos.ofFloored(packet.center().getX(), packet.center().getY(), packet.center().getZ());
            if (surround.contains(pos))
            {
                final int slot = getResistantBlockItem();
                if (slot == -1) return;
                placeBlock(pos, slot);
            }
        }

        if (serverPacket instanceof EntitySpawnS2CPacket packet
            && packet.getEntityType().equals(EntityType.END_CRYSTAL) && prePlaceTick.get())
        {
            for (BlockPos pos : surround)
            {
                if (!pos.equals(BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ()))) continue;

                final int slot = getResistantBlockItem();
                if (slot == -1) return;
                placeBlock(pos, slot);
                break;
            }
        }
    }

    private List<BlockPos> getAdjacentPlacements(BlockPos minedPos)
    {
        List<BlockPos> result = new ArrayList<>();
        for (Direction dir : Direction.values())
        {
            BlockPos adjacent = minedPos.offset(dir);

            if (adjacent.equals(mc.player.getBlockPos()) || adjacent.equals(mc.player.getBlockPos().up())) continue;
            if (!mc.world.getBlockState(adjacent).isReplaceable()) continue;
            if (!mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, adjacent, ShapeContext.absent())) continue;

            result.add(adjacent);
        }
        return result;
    }

    private void placeBlock(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirection.get(), false, true, (state, angles) ->
        {
            if (rotate.get() && state) {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
        blocksPlaced++;
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
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
        }
    }

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround)
    {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround)
        {
            Long placed = packets.get(surroundPos);
            if (shiftDelay.get() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelay.get() * 50.0f)
            {
                continue;
            }
            if (!mc.world.getBlockState(surroundPos).isReplaceable())
            {
                continue;
            }
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (dist > MathUtil.squared(placeRange.get()))
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

    public List<BlockPos> getSurround(PlayerEntity player)
    {
        List<BlockPos> surroundBlocks = getSurroundNoDown(player);
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos playerPos : playerBlocks)
        {
            if (playerPos.equals(player.getBlockPos()))
            {
                continue;
            }
            surroundBlocks.add(playerPos.down());
        }
        if (mineExtend.get())
        {
            for (BlockPos surroundPos : new ArrayList<>(surroundBlocks))
            {
                if (!Managers.BLOCK.isPassed(surroundPos, 0.7f)) continue;

                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) continue;

                    BlockPos blockerPos = surroundPos.offset(direction);
                    if (playerBlocks.contains(blockerPos)
                        || Modules.get().get(GenyoAutoMine.class).getMiningBlock() == blockerPos) // Dont want to help our opponent surround
                    {
                        continue;
                    }
                    surroundBlocks.add(blockerPos);
                }
            }
        }

        /*if (AirPlaceModule.getInstance().isEnabled() && head.get())
        {
            surroundBlocks.add(mc.player.getBlockPos().up(2));
        }*/
        return surroundBlocks;
    }

    public List<BlockPos> getSurroundNoDown(PlayerEntity player)
    {
        return getSurroundNoDown(player, 0.0f);
    }

    public List<BlockPos> getSurroundNoDown(PlayerEntity player, float range)
    {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos pos : playerBlocks)
        {
            if (range > 0.0f && mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range * range)
            {
                continue;
            }
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
            }
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(PlayerEntity entity)
    {
        BlockPos playerPos = GPositionUtils.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extend.get())
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
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null && mc.player == null) return;

        if (render.get())
        {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());

                Color boxColor = color.get().a(boxAlpha);
                Color lineColor = color.get().a(lineAlpha);

                event.renderer.box(set.getKey(), boxColor, lineColor, ShapeMode.Both, 1);
            }

            if (placements.isEmpty())
            {
                return;
            }

            for (BlockPos pos : placements)
            {
                Animation animation = new Animation(true, fadeTime.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    public boolean isPlacing()
    {
        return !placements.isEmpty();
    }

    public boolean isInPlacements(BlockPos pos) {
        return placements.contains(pos);
    }

    public enum Timing
    {
        VANILLA,
        SEQUENTIAL
    }
    private boolean isAirPlace(BlockPos blockPos) {
        for (Direction dir : Direction.values()) {
            if (!mc.world.getBlockState(blockPos.offset(dir)).isReplaceable()) return false;
        }
        return true;
    }
    private float square(float value) {
        return value*value;
    }

}
