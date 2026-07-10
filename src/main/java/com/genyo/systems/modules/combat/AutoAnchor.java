package com.genyo.systems.modules.combat;

import com.genyo.Genyo;
import com.genyo.managers.Managers;
import com.genyo.render.animation.Animation;
import com.genyo.systems.modules.PlacerModule;
import com.genyo.systems.settings.FloatSetting;
import com.genyo.utils.entity.EntityUtil;
import com.genyo.utils.math.timer.CacheTimer;
import com.genyo.utils.math.timer.Timer;
import com.genyo.utils.player.InventoryUtil;
import com.genyo.utils.player.RotationUtil;
import com.genyo.utils.world.ExplosionUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.AirPlace;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class AutoAnchor extends PlacerModule {

    public AutoAnchor() {
        super(Genyo.COMBAT, "auto-anchor", "Automatically places and explodes respawn anchors");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgBehaviour = settings.createGroup("Behaviour");
    private final SettingGroup sgRender = settings.createGroup("Render");

    /**
     * General
     */

    private final Setting<Float> targetRange = sgGeneral.add(new FloatSetting.Builder()
        .name("enemy-range")
        .description("Range to search for potential enemies")
        .min(1f).defaultValue(10f).max(13f)
        .sliderRange(1f, 13f)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing hand when exploding anchors")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate before exploding")
        .defaultValue(false)
        .build()
    );

    /**
     * Target
     */

    private final Setting<Boolean> players = sgTarget.add(new BoolSetting.Builder()
        .name("players")
        .description("Target players")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> monsters = sgTarget.add(new BoolSetting.Builder()
        .name("monsters")
        .description("Target monsters")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> neutrals = sgTarget.add(new BoolSetting.Builder()
        .name("neutrals")
        .description("Target neutrals")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> animals = sgTarget.add(new BoolSetting.Builder()
        .name("animals")
        .description("Target animals")
        .defaultValue(false)
        .build()
    );

    /**
     * Behaviour
     */

    private final Setting<Float> range = sgBehaviour.add(new FloatSetting.Builder()
        .name("range")
        .description("Range to explode anchors")
        .min(0.1f).defaultValue(4f).max(6f)
        .sliderRange(0.1f, 6f)
        .build()
    );

    private final Setting<Float> explodeSpeed = sgBehaviour.add(new FloatSetting.Builder()
        .name("explode-speed")
        .description("Speed to explode anchors")
        .min(0.1f).defaultValue(18f).max(20f)
        .sliderRange(0.1f, 20f)
        .build()
    );

    private final Setting<Boolean> place = sgBehaviour.add(new BoolSetting.Builder()
        .name("place")
        .description("Places anchors to damage enemies")
        .defaultValue(true)
        .build()
    );

    private final Setting<Float> placeSpeed = sgBehaviour.add(new FloatSetting.Builder()
        .name("place-speed")
        .description("Speed to place anchors")
        .min(0.1f).defaultValue(18f).max(20f)
        .sliderRange(0.1f, 20f)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> strictDirection = sgBehaviour.add(new BoolSetting.Builder()
        .name("strict-direction")
        .description("Interacts with only visible directions when placing crystals")
        .defaultValue(false)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> grim = sgBehaviour.add(new BoolSetting.Builder()
        .name("grim")
        .description("Places using grim instant rotations")
        .defaultValue(false)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> assumeArmor = sgBehaviour.add(new BoolSetting.Builder()
        .name("assume-best-armor")
        .description("Assumes Prot 0 armor is max armor")
        .defaultValue(false)
        .build()
    );

    private final Setting<Float> minDamage = sgBehaviour.add(new FloatSetting.Builder()
        .name("min-damage")
        .description("Minimum damage required to consider exploding anchors")
        .min(1f).defaultValue(4f).max(10f)
        .sliderRange(1f, 10f)
        .build()
    );

    private final Setting<Boolean> safety = sgBehaviour.add(new BoolSetting.Builder()
        .name("safety")
        .description("Accounts for total player safety when exploding anchors")
        .defaultValue(true)
        .build()
    );

    private final Setting<Float> maxLocalDamage = sgBehaviour.add(new FloatSetting.Builder()
        .name("max-local-damage")
        .description("The maximum player damage")
        .min(4f).defaultValue(12f).max(20f)
        .sliderRange(4f, 20f)
        .build()
    );

    private final Setting<Boolean> blockDestruction = sgBehaviour.add(new BoolSetting.Builder()
        .name("block-destruction")
        .description("Accounts for explosion block destruction when calculating damages")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> selfExtrapolate = sgBehaviour.add(new BoolSetting.Builder()
        .name("self-extrapolate")
        .description("Accounts for motion when calculating self damage")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> extrapolateTicks = sgBehaviour.add(new IntSetting.Builder()
        .name("extrapolation-ticks")
        .description("Accounts for motion when calculating enemy positions, not fully accurate.")
        .min(0).defaultValue(0).max(10)
        .sliderRange(0, 10)
        .build()
    );

    /**
     * Render
     */

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders where anchors will be placed")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Timer for the fade")
        .min(0).defaultValue(250).max(1000)
        .sliderRange(0, 1000)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Render color")
        .defaultValue(new Color(236, 243, 122, 40))
        .build()
    );

    private AnchorCalc anchorCalc;
    private final Timer explodeTimer = new CacheTimer();
    private final Timer placeTimer = new CacheTimer();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();

    @Override
    public void onDeactivate()
    {
        anchorCalc = null;
        fadeList.clear();
    }

    @EventHandler
    public void onPlayerTick(TickEvent.Pre event)
    {
        if (!InventoryUtil.hasItemInHotbar(Items.RESPAWN_ANCHOR) || !InventoryUtil.hasItemInHotbar(Items.GLOWSTONE))
        {
            anchorCalc = null;
            return;
        }

        if (!multitask.get() && checkMultitask())
        {
            anchorCalc = null;
            return;
        }

        anchorCalc = calculateAnchorExplosion();
        if (anchorCalc == null)
        {
            return;
        }

        final BlockPos anchorPos = anchorCalc.pos();
        if (anchorCalc.isAnchor())
        {
            if (rotate.get())
            {
                float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), anchorPos.toCenterPos());
                setRotation(rotations[0], rotations[1]);
            }
            if (explodeTimer.passed(1000.0f - explodeSpeed.get() * 50.0f))
            {
                setAnchor(anchorPos);
                explodeTimer.reset();
            }
        }
        else
        {
            if (mc.world.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) return;
            int slot = getBlockItemSlot(Blocks.RESPAWN_ANCHOR);
            if (slot == -1) return;
            if (placeTimer.passed(1000.0f - placeSpeed.get() * 50.0f)) {
                Managers.INVENTORY.setSlot(slot);
                Managers.INTERACT.placeBlock(anchorPos, slot, grim.get(), strictDirection.get(), false, (state, angles) ->
                {
                    if (rotate.get()) {
                        if (state) Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                        else {
                            if (grim.get()) Managers.ROTATION.setRotationSilentSync();
                        }
                    }
                });
                Managers.INVENTORY.syncToClient();
                placeTimer.reset();
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
        {
            if (anchorCalc != null && set.getKey() == anchorCalc.pos())
            {
                continue;
            }
            set.getValue().setState(false);
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());

            Color boxColor = color.get().a(boxAlpha);
            Color lineColor = color.get().a(lineAlpha);

            event.renderer.box(set.getKey(), lineColor, boxColor, ShapeMode.Both, 0);
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);

        if (anchorCalc != null)
        {
            Animation animation = new Animation(true, fadeTime.get());
            fadeList.put(anchorCalc.pos(), animation);
        }
    }

    private void setAnchor(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof RespawnAnchorBlock))
        {
            return;
        }
        int slot1 = findNonBlockSlot();
        if (slot1 == -1)
        {
            return;
        }
        int charges = state.get(RespawnAnchorBlock.CHARGES);
        if (charges <= 0)
        {
            int slot = getBlockItemSlot(Blocks.GLOWSTONE);
            if (slot == -1)
            {
                return;
            }
            Managers.INVENTORY.setSlot(slot);
            BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, strictDirection.get()), pos, true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.INVENTORY.setSlot(slot1);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
            if (swing.get())
            {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            else
            {
                Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            Managers.INVENTORY.syncToClient();
        }
        else
        {
            Managers.INVENTORY.setSlot(slot1);
            BlockHitResult result1 = new BlockHitResult(pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, strictDirection.get()), pos, true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result1);
            if (swing.get())
            {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
            else
            {
                Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            Managers.INVENTORY.syncToClient();
        }
    }

    private AnchorCalc calculateAnchorExplosion()
    {
        // explosion
        BlockPos data = null;
        double bestAnchorDamage = 0.0f;
        boolean isAnchor = false;

        for (BlockPos pos : getSphere(mc.player.getEntityPos()))
        {
            BlockState state = mc.world.getBlockState(pos);
            double dist1 = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
            if (dist1 > (range.get() * range.get()))
            {
                continue;
            }

            boolean explosion = state.getBlock() instanceof RespawnAnchorBlock;
            if (state.isReplaceable() || explosion)
            {
                double selfDamage = ExplosionUtil.getDamageTo(mc.player,
                    pos.toCenterPos(), blockDestruction.get(), 10.0f, Set.of(pos), selfExtrapolate.get() ? extrapolateTicks.get() : 0, false); // Anchor explosions power = 10
                boolean unsafeToPlayer = playerDamageCheck(selfDamage);
                if (unsafeToPlayer)
                {
                    continue;
                }

                if (!Modules.get().isActive(AirPlace.class)
                    && Managers.INTERACT.getInteractDirectionInternal(pos, false) == null)
                {
                    continue;
                }

                for (Entity entity : mc.world.getEntities())
                {
                    if (entity.getBoundingBox().intersects(new Box(pos)))
                    {
                        continue;
                    }

                    if (entity == null || !entity.isAlive() || entity == mc.player
                        || !isValidTarget(entity)
                        || Managers.SOCIAL.isFriend(entity.getName().getLiteralString()))
                    {
                        continue;
                    }

                    double blockDist = pos.getSquaredDistance(entity.getBlockPos());
                    if (blockDist > 144.0f)
                    {
                        continue;
                    }
                    double dist = mc.player.squaredDistanceTo(entity);
                    if (dist > targetRange.get() * targetRange.get())
                    {
                        continue;
                    }
                    double damage = ExplosionUtil.getDamageTo(entity,
                        pos.toCenterPos(), blockDestruction.get(), 10.0f, Set.of(pos), extrapolateTicks.get(), assumeArmor.get());
                    if (damage > bestAnchorDamage)
                    {
                        data = pos;
                        bestAnchorDamage = damage;
                        isAnchor = explosion;
                    }
                }
            }
        }

        if (data != null && bestAnchorDamage >= minDamage.get())
        {
            return new AnchorCalc(data, isAnchor);
        }

        return null;
    }

    private boolean playerDamageCheck(double playerDamage)
    {
        if (!mc.player.isCreative())
        {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (safety.get() && playerDamage >= health + 0.5f)
            {
                return true;
            }
            return playerDamage > maxLocalDamage.get();
        }
        return false;
    }

    private List<BlockPos> getSphere(Vec3d origin)
    {
        List<BlockPos> sphere = new ArrayList<>();
        double rad = Math.ceil(range.get());
        for (double x = -rad; x <= rad; ++x)
        {
            for (double y = -rad; y <= rad; ++y)
            {
                for (double z = -rad; z <= rad; ++z)
                {
                    Vec3i pos = new Vec3i((int) (origin.getX() + x),
                        (int) (origin.getY() + y), (int) (origin.getZ() + z));
                    final BlockPos p = new BlockPos(pos);
                    sphere.add(p);
                }
            }
        }
        return sphere;
    }

    private boolean isValidTarget(Entity e)
    {
        return e instanceof PlayerEntity && players.get()
            || EntityUtil.isMonster(e) && monsters.get()
            || EntityUtil.isNeutral(e) && neutrals.get()
            || EntityUtil.isPassive(e) && animals.get();
    }

    private int findNonBlockSlot()
    {
        int slot = -1;
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem)
            {
                continue;
            }
            slot = i;
            break;
        }
        return slot;
    }

    private record AnchorCalc(BlockPos pos, boolean isAnchor) {}

}
