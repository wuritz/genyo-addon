package com.genyo.systems.modules.visual;


import com.genyo.render.FrozenPlayerRenderState;
import com.genyo.render.PlayerWireframeRenderer;
import com.genyo.render.RenderStateCache;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.player.PlayerSkinType;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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
        .defaultValue(new SettingColor(255, 0, 255))
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
        for (Spot spot : spots) {
            spot.render(event, lineColor.get());
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

        public void render(Render3DEvent event, meteordevelopment.meteorclient.utils.render.color.Color color) {
            PlayerWireframeRenderer.render(event, modelPart, state.x, state.y, state.z, state.bodyYaw, color);
        }
    }
}
