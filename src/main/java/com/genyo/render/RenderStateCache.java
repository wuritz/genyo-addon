package com.genyo.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.orbit.EventHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RenderStateCache {
    private static final Map<UUID, FrozenPlayerRenderState> cache = new ConcurrentHashMap<>();

    static {
        MeteorClient.EVENT_BUS.subscribe(new Object() {
            @EventHandler
            private void onGameLeft(GameLeftEvent event) {
                cache.clear();
            }
        });
    }

    public static void put(UUID uuid, FrozenPlayerRenderState state) {
        cache.put(uuid, state);
    }

    public static FrozenPlayerRenderState freeze(UUID uuid) {
        return cache.get(uuid);
    }

    public static void clear(UUID uuid) {
        cache.remove(uuid);
    }
}
