package com.genyo.systems.modules.combat;

import com.genyo.Genyo;
import com.genyo.events.network.PlayerTickEvent;
import com.genyo.managers.Managers;
import com.genyo.mixin.accessor.AccessorCrystalAura;
import com.genyo.render.animation.Animation;
import com.genyo.systems.modules.PlacerModule;
import com.genyo.systems.settings.FloatSetting;
import com.genyo.utils.GEntityUtils;
import com.genyo.utils.math.MathUtil;
import com.genyo.utils.world.ExplosionUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;

import meteordevelopment.meteorclient.systems.modules.player.AirPlace;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasePlace extends PlacerModule {

    public BasePlace() {
        super(Genyo.COMBAT, "base-place", "Places obsidian for crystal placements");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to block before placing")
        .defaultValue(false)
        .build()
    );

    private final Setting<Float> placeRange = sgGeneral.add(new FloatSetting.Builder()
        .name("place-range")
        .description("The placement range for bases")
        .min(0f).defaultValue(4f).max(6f)
        .sliderRange(0f, 6f)
        .build()
    );

    private final Setting<Float> enemyRange = sgGeneral.add(new FloatSetting.Builder()
        .name("enemy-range")
        .description("The maximum range of targets")
        .min(0.1f).defaultValue(10f).max(15f)
        .sliderRange(0.1f, 15f)
        .build()
    );

    private final Setting<Float> shiftDelay = sgGeneral.add(new FloatSetting.Builder()
        .name("shift-delay")
        .description("The delay between each block placement interval")
        .min(0f).defaultValue(1f).max(5f)
        .sliderRange(0f, 5f)
        .build()
    );

    private final Setting<Float> minDamage = sgGeneral.add(new FloatSetting.Builder()
        .name("min-damage")
        .description("Minimum damage required to place base")
        .min(1f).defaultValue(4f).max(10f)
        .sliderRange(1f, 10f)
        .build()
    );

    private final Setting<Boolean> assumeArmor = sgGeneral.add(new BoolSetting.Builder()
        .name("assume-best-armor")
        .description("Assumes Prot 0 armor is max armor")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders web placements")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> renderColor = sgRender.add(new ColorSetting.Builder()
        .name("render-color")
        .description("The render color")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> fadeTime = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time to fade")
        .min(0).defaultValue(250).max(1000)
        .sliderRange(0, 1000)
        .visible(render::get)
        .build()
    );

    private BlockPos crystalBase;
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();

    @Override
    public void onDeactivate() {
        crystalBase = null;
        packets.clear();
        fadeList.clear();
    }

    @EventHandler
    public void onTick(PlayerTickEvent event)
    {
        boolean genyoActive = Modules.get().isActive(GenyoAutoCrystal.class);
        boolean caActive = Modules.get().isActive(CrystalAura.class);

        // Neither aura running -> nothing for base-place to support
        if (!genyoActive && !caActive)
        {
            return;
        }

        // Don't interfere while the active aura is itself mid-placement
        if (genyoActive && Modules.get().get(GenyoAutoCrystal.class).isPlacing())
        {
            return;
        }

        if (caActive && ((AccessorCrystalAura) Modules.get().get(CrystalAura.class)).isPlacing())
        {
            return;
        }

        PlayerEntity target = getClosestPlayer(enemyRange.get());
        if (target == null)
        {
            return;
        }

        crystalBase = getCrystalBase(target);
        if (crystalBase == null)
        {
            return;
        }

        BlockState state = mc.world.getBlockState(crystalBase);
        int slot = getResistantBlockItem();
        if (slot == -1 || !state.isReplaceable())
        {
            return;
        }

        placeBlock(crystalBase, slot);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (render.get())
        {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet())
            {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                Color boxColor = renderColor.get().a(boxAlpha);
                Color lineColor = renderColor.get().a(lineAlpha);
                event.renderer.box(set.getKey(), boxColor, lineColor, ShapeMode.Both, 1);
            }

            if (crystalBase != null && mc.world.isAir(crystalBase))
            {
                Animation animation = new Animation(true, fadeTime.get());
                fadeList.put(crystalBase, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    private void placeBlock(BlockPos pos, int slot)
    {
        Managers.INTERACT.placeBlock(pos, slot, strictDirection.get(), false, true, (state, angles) ->
        {
            if (rotate.get())
            {
                if (state)
                {
                    Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                }
                else
                {
                    Managers.ROTATION.setRotationSilentSync();
                }
            }
        });
        packets.put(pos, System.currentTimeMillis());
    }

    private BlockPos getCrystalBase(PlayerEntity player)
    {
        List<BlockPos> targetBlocks = getSphere(placeRange.get(), mc.player.getEyePos());
        double damage = 0.0f;
        BlockPos crystalBase = null;
        for (BlockPos pos : targetBlocks)
        {
            final BlockPos basePos = pos.down();
            if (basePos.getY() >= GEntityUtils.getRoundedBlockPos(player).getY())
            {
                continue;
            }

            Long placed = packets.get(basePos);
            if (shiftDelay.get() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelay.get() * 50.0f)
            {
                continue;
            }

            if (Modules.get().isActive(GenyoAutoCrystal.class)
                && !Modules.get().get(GenyoAutoCrystal.class).isCrystalHitboxClear(pos))
            {
                continue;
            }

            double dist = mc.player.squaredDistanceTo(basePos.toCenterPos());
            if (dist > MathUtil.squared(placeRange.get()))
            {
                continue;
            }

            double dmg1 = ExplosionUtil.getDamageTo(player, pos.toCenterPos(), assumeArmor.get());
            if (dmg1 < minDamage.get())
            {
                continue;
            }

            if (!Modules.get().isActive(AirPlace.class)
                && Managers.INTERACT.getInteractDirectionInternal(basePos, strictDirection.get()) == null)
            {
                continue;
            }

            if (!mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, basePos, ShapeContext.absent()))
            {
                continue;
            }

            if (dmg1 > damage)
            {
                crystalBase = basePos;
                damage = dmg1;
            }
        }

        return crystalBase;
    }

    private List<BlockPos> getSphere(double rad, Vec3d origin)
    {
        List<BlockPos> sphere = new ArrayList<>();
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

}
