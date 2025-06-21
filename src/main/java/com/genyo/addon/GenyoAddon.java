package com.genyo.addon;

import com.genyo.addon.hud.PvPNeccessaryHud;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class GenyoAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Example");
    public static final HudGroup HUD_GROUP = new HudGroup("Genyo");

    @Override
    public void onInitialize() {
        LOG.info("Genyo fasz indul genyo");

        // HUD
        Hud.get().register(PvPNeccessaryHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.genyo.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("wuritz", "genyo-addon");
    }
}
