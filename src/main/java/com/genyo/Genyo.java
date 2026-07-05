package com.genyo;

import com.genyo.commands.DiscordTurnOffCommand;
import com.genyo.commands.ExportCommand;
import com.genyo.core.Core;
import com.genyo.commands.EnemiesCommand;
import com.genyo.systems.config.GenyoConfig;
import com.genyo.systems.config.GenyoTab;
import com.genyo.systems.hud.*;
import com.genyo.systems.modules.combat.*;
import com.genyo.systems.modules.misc.*;
import com.genyo.systems.modules.movement.*;
import com.genyo.systems.modules.visual.*;
import com.genyo.systems.enemies.EnemiesTab;
import com.genyo.managers.Managers;
import com.genyo.systems.enemies.Enemies;
import com.genyo.systems.modules.world.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.meteorclient.utils.misc.Version;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.item.Items;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Genyo extends MeteorAddon {

    public static final Logger LOG = LogUtils.getLogger();

    // Categories
    public static final Category COMBAT = new Category("G-COMBAT", Items.MILK_BUCKET.getDefaultStack());
    public static final Category MISC = new Category("G-MISC", Items.MILK_BUCKET.getDefaultStack());
    public static final Category MOVEMENT = new Category("G-MOVE", Items.MILK_BUCKET.getDefaultStack());
    public static final Category VISUAL = new Category("G-VISUAL", Items.MILK_BUCKET.getDefaultStack());
    public static final Category WORLD = new Category("G-WORLD", Items.MILK_BUCKET.getDefaultStack());

    public static final List<Category> CATEGORIES = new ArrayList<>();

    public static final HudGroup HUD_GROUP = new HudGroup("Genyo");
    public static final List<HudElementInfo<?>> HUD_ELEMENTS = new ArrayList<>();

    public static final String MOD_ID = "genyo";
    public static final ModMetadata MOD_META;
    public static final String NAME;
    public static final Version VERSION;

    public static Core core;

    static {
        MOD_META = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();

        NAME = MOD_META.getName();

        String versionString = MOD_META.getVersion().getFriendlyString();
        if (versionString.contains("-")) versionString = versionString.split("-")[0];

        // When building and running through IntelliJ and not Gradle it doesn't replace the version so just use a dummy
        if (versionString.equals("${version}")) versionString = "0.0.0";

        VERSION = new Version(versionString);
    }

    @Override
    public void onInitialize() {
        LOG.info("Welcome to Genyo :D");

        if (Modules.get().isActive(DiscordPresence.class)
            && GenyoConfig.get().genyoDiscord.get())
        {
            Modules.get().get(DiscordPresence.class).toggle();
            LOG.info("oh no la policia");
        }

        // Tabs
        initTabs();

        // Commands
        Commands.add(new EnemiesCommand());
        Commands.add(new ExportCommand());
        Commands.add(new DiscordTurnOffCommand());

        // Systems
        initSystems();

        initModules(Modules.get());

        // Managers mert ez menőn néz ki
        Managers.init();

        // HUD
        initHUD(Hud.get());

        // For export
        CATEGORIES.add(COMBAT);
        CATEGORIES.add(MISC);
        CATEGORIES.add(MOVEMENT);
        CATEGORIES.add(VISUAL);
        CATEGORIES.add(WORLD);

        core = new Core();
    }

    private void initTabs() {
        Tabs.add(new EnemiesTab());
        Tabs.add(new GenyoTab());
    }

    private void initSystems() {
        Systems.add(new Enemies());
        Systems.add(new GenyoConfig());
    }

    private void initModules(Modules modules) {
        modules.add(new GenyoAutoEZ());
        modules.add(new GenyoAutoPortal());
        modules.add(new GenyoMineESP());
        modules.add(new GenyoNuker());
        modules.add(new GenyoNoSwing());
        modules.add(new GenyoSurround());
        modules.add(new GenyoWelcome());
        modules.add(new GenyoSkinBlink());
        modules.add(new GenyoGoodbye());
        modules.add(new GenyoSurroundV2());
        modules.add(new GenyoAutoCrystal());
        modules.add(new GenyoDiscord());
        modules.add(new GenyoSpeedmine());
        modules.add(new GenyoAutoTool());
        modules.add(new GenyoReplenish());
        modules.add(new GenyoScaffold());
        modules.add(new PenisESP());
        modules.add(new GenyoAutoTotem());
        modules.add(new GenyoVelocity());
        modules.add(new KFCSpawnKill());
        modules.add(new GenyoCriticals());
        modules.add(new GenyoGhostBlocks());
        modules.add(new GenyoSelfTrap());
        modules.add(new CombatBrainrot());
        modules.add(new PacketDebug());
        modules.add(new GenyoAutoMine());
        modules.add(new GenyoAutoMineV2());
        modules.add(new GenyoAutoXP());
        modules.add(new GenyoAutoArmor());
        modules.add(new GenyoAutoTrap());
        modules.add(new GenyoCapes());
        modules.add(new GenyoPhase());
        //modules.add(new GenyoMainMenu());
        modules.add(new GenyoTimer());
        modules.add(new AutoSigma());
        modules.add(new Einstein());
        modules.add(new GenyoAutoWindmill());
        modules.add(new GenyoAutoPenis());
        modules.add(new NoPacketKick());
        modules.add(new AntiLadder());
        modules.add(new AutoBrainrot());
        modules.add(new GenyoNoSlow());
        //modules.add(new TsunodaBlinker());
        modules.add(new GenyoSwing());
        modules.add(new GenyoNametags());
        //modules.add(new AutoOminous());
        modules.add(new FastLatency());
        modules.add(new FastPlace());
        modules.add(new AutoWeb());
        modules.add(new AutoCrawlTrap());
        modules.add(new BasePlace());
        modules.add(new AutoRename());
        modules.add(new FriendProtector());
        modules.add(new Parkinsons());
    }

    private void initHUD(Hud hud) {
        hud.register(PvPNeccessaryHud.INFO);
        hud.register(ActiveGenyoHud.INFO);
        hud.register(PacketsHud.INFO);
        hud.register(WatermarkHud.INFO);
        hud.register(BetterPlayerRadarHud.INFO);
        hud.register(LogoHud.INFO);
        hud.register(PingHud.INFO);
        hud.register(GenyoTargetHud.INFO);

        HUD_ELEMENTS.add(PvPNeccessaryHud.INFO);
        HUD_ELEMENTS.add(ActiveGenyoHud.INFO);
        HUD_ELEMENTS.add(PacketsHud.INFO);
        HUD_ELEMENTS.add(WatermarkHud.INFO);
        HUD_ELEMENTS.add(BetterPlayerRadarHud.INFO);
        HUD_ELEMENTS.add(LogoHud.INFO);
        HUD_ELEMENTS.add(PingHud.INFO);
        HUD_ELEMENTS.add(GenyoTargetHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(COMBAT);
        Modules.registerCategory(MISC);
        Modules.registerCategory(MOVEMENT);
        Modules.registerCategory(VISUAL);
        Modules.registerCategory(WORLD);
    }

    @Override
    public String getPackage() {
        return "com.genyo";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("wuritz", "genyo-addon");
    }

    @Override
    public String getWebsite() {
        return "https://genyo.dev";
    }

    @Override
    public String getCommit() {
        ModMetadata modMetadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
        String commit = modMetadata.getCustomValue("github:sha").getAsString();
        return commit.isEmpty() ? null : commit;
    }

}
