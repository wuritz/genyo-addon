package com.genyo.systems.enemies;

import com.genyo.Genyo;
import com.mojang.util.UndashedUuid;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.network.FailedHttpResponse;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Enemy implements ISerializable<Enemy>, Comparable<Enemy> {

    public volatile String name;
    private volatile @Nullable UUID id;
    private volatile @Nullable PlayerHeadTexture headTexture;
    private volatile boolean updating;

    public Enemy(String name, @Nullable UUID id) {
        this.name = name;
        this.id = id;
        this.headTexture = null;
    }

    public Enemy(PlayerEntity player) {
        this(player.getName().getString(), player.getUuid());
    }

    public Enemy(String name) {
        this(name, null);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlayerHeadTexture getHead() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void updateInfo() {
        updating = true;
        HttpResponse<APIResponse> res = null;

        if (id != null) {
            res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UndashedUuid.toString(id))
                .exceptionHandler(ex -> Genyo.LOG.error("Error while trying to connect session server for friend '{}'", name))
                .sendJsonResponse(APIResponse.class);
        }

        // Fallback to name-based lookup
        if (res == null || res.statusCode() != 200) {
            res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + name)
                .exceptionHandler(ex -> Genyo.LOG.error("Error while trying to update info for friend '{}'", name))
                .sendJsonResponse(APIResponse.class);
        }

        if (res != null && res.statusCode() == 200) {
            name = res.body().name;
            id = UndashedUuid.fromStringLenient(res.body().id);

            byte[] head = PlayerHeadUtils.fetchHead(id);
            mc.execute(() -> {
                if (head != null) headTexture = new PlayerHeadTexture(head, true);
            });
        }

        // cracked accounts shouldn't be assigned ids
        else if (!(res instanceof FailedHttpResponse)) {
            id = null;
        }

        updating = false;
    }

    public boolean headTextureNeedsUpdate() {
        return !this.updating && headTexture == null;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        if (id != null) tag.putString("id", UndashedUuid.toString(id));

        return tag;
    }

    @Override
    public Enemy fromTag(NbtCompound tag) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enemy enemy = (Enemy) o;
        return Objects.equals(name, enemy.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NotNull Enemy enemy) {
        return name.compareTo(enemy.name);
    }

    private static class APIResponse {
        String name, id;
    }

}
