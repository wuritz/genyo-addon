package com.genyo.systems.modules.visual;


import com.genyo.render.FrozenPlayerRenderState;
import com.genyo.render.PlayerWireframeRenderer;
import com.genyo.render.RenderStateCache;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.player.PlayerSkinType;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GenyoLogoutSpots extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Wireframe line color.")
        .defaultValue(new SettingColor(85, 220, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color for the wireframe's side faces.")
        .defaultValue(new SettingColor(255, 0, 255))
        .build()
    );

    private final Setting<Integer> sideOpacity = sgRender.add(new IntSetting.Builder()
        .name("side-opacity")
        .description("Opacity of the side face fill (0 = invisible, 255 = solid).")
        .defaultValue(30)
        .min(0)
        .max(255)
        .sliderMin(0)
        .sliderMax(255)
        .build()
    );

    private final Setting<Double> lineThickness = sgRender.add(new DoubleSetting.Builder()
        .name("line-thickness")
        .description("Thickness of the wireframe lines, in blocks.")
        .defaultValue(0.150)
        .min(0.005)
        .sliderMin(0.005)
        .sliderMax(0.15)
        .build()
    );

    private final Setting<Boolean> showNames = sgRender.add(new BoolSetting.Builder()
        .name("show-names")
        .description("Renders the player's name above their logout spot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("Color of the player name text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(showNames::get)
        .build()
    );

    private final Setting<Integer> maxSpots = sgGeneral.add(new IntSetting.Builder()
        .name("max-spots")
        .description("Maximum number of logout spots to keep at once.")
        .defaultValue(20)
        .min(1)
        .sliderMin(1)
        .build()
    );

    private final List<Spot> spots = new ArrayList<>();

    private final List<PlayerListEntry> lastTabList = new ArrayList<>();
    private final List<PlayerEntity> lastLoadedPlayers = new ArrayList<>();

    private int timer;
    private DimensionType lastDimension;

    public GenyoLogoutSpots() {
        super(Categories.Render, "genyo-logout-spots", "Freezes an exact wireframe of a player at the moment they log out.");
    }

    @Override
    public void onActivate() {
        spots.clear();
        lastTabList.clear();
        lastTabList.addAll(mc.getNetworkHandler().getPlayerList());
        refreshLoadedPlayers();

        timer = 10;
        lastDimension = mc.world.getDimension();
    }

    @Override
    public void onDeactivate() {
        spots.clear();
        lastTabList.clear();
    }

    private void refreshLoadedPlayers() {
        lastLoadedPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player != mc.player) {
                lastLoadedPlayers.add(player);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        var currentTab = mc.getNetworkHandler().getPlayerList();

        if (currentTab.size() != lastTabList.size()) {
            for (PlayerListEntry oldEntry : lastTabList) {
                UUID uuid = oldEntry.getProfile().id();

                boolean stillInTab = currentTab.stream()
                    .anyMatch(e -> e.getProfile().id().equals(uuid));

                if (!stillInTab) {
                    for (PlayerEntity real : lastLoadedPlayers) {
                        if (real.getUuid().equals(uuid)) {
                            addSpot(real);
                            break;
                        }
                    }
                }
            }

            lastTabList.clear();
            lastTabList.addAll(currentTab);
        }

        if (timer <= 0) {
            refreshLoadedPlayers();
            timer = 10;
        } else {
            timer--;
        }

        DimensionType dimension = mc.world.getDimension();
        if (dimension != lastDimension) spots.clear();
        lastDimension = dimension;
    }

    @EventHandler
    private void onEntityAdded(meteordevelopment.meteorclient.events.entity.EntityAddedEvent event) {
        // Player reconnected/came back into range — drop the stale spot for them
        if (event.entity instanceof PlayerEntity real) {
            spots.removeIf(spot -> spot.uuid.equals(real.getUuid()));
        }
    }

    private void addSpot(PlayerEntity real) {
        FrozenPlayerRenderState frozen = RenderStateCache.freeze(real.getUuid());
        if (frozen == null) return; // no captured frame yet (e.g. player never rendered on screen), nothing to freeze

        spots.removeIf(s -> s.uuid.equals(real.getUuid()));
        spots.add(new Spot(real.getUuid(), real.getName().getString(), frozen));

        while (spots.size() > maxSpots.get()) {
            spots.remove(0);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        SettingColor side = sideColor.get();
        Color sideWithOpacity = new SettingColor(side.r, side.g, side.b, sideOpacity.get());
        Color line = lineColor.get();
        double thickness = lineThickness.get();

        for (Spot spot : spots) {
            spot.render(event, line, sideWithOpacity, thickness);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!showNames.get() || spots.isEmpty()) return;

        Color color = nameColor.get();

        for (Spot spot : spots) {
            // Head height roughly matches the frozen state's standing eye height
            Vec3d worldPos = new Vec3d(spot.state.x, spot.state.y + spot.state.standingEyeHeight + 0.3, spot.state.z);

            Vec3d screen = mc.gameRenderer.project(worldPos);
            // project() returns NDC-like coords; z outside [0,1] roughly means behind/outside camera
            if (screen.z < 0 || screen.z > 1) continue;

            int x = (int) ((screen.x * 0.5 + 0.5) * mc.getWindow().getScaledWidth());
            int y = (int) ((1.0 - (screen.y * 0.5 + 0.5)) * mc.getWindow().getScaledHeight());

            DrawContext ctx = event.drawContext;
            int textWidth = mc.textRenderer.getWidth(spot.name);
            ctx.drawText(mc.textRenderer, spot.name, x - textWidth / 2, y, color.getPacked(), true);
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(spots.size());
    }

    public List<Spot> getSpots() {
        return spots;
    }

    public static class Spot {
        public final UUID uuid;
        public final String name;
        public final FrozenPlayerRenderState state;

        private final ModelPart modelPart;
        private final BipedEntityModel<PlayerEntityRenderState> model;

        @SuppressWarnings("unchecked")
        public Spot(UUID uuid, String name, FrozenPlayerRenderState state) {
            this.uuid = uuid;
            this.name = name;
            this.state = state;

            this.modelPart = net.minecraft.client.MinecraftClient.getInstance()
                .getLoadedEntityModels()
                .getModelPart(EntityModelLayers.PLAYER);

            this.model = new BipedEntityModel<>(modelPart);
            this.model.setAngles(state);
        }

        public void render(Render3DEvent event, Color lineColor, Color sideColor, double lineThickness) {
            PlayerWireframeRenderer.render(event, modelPart, state.x, state.y, state.z, state.bodyYaw,
                lineColor, sideColor, lineThickness);
        }
    }
}
