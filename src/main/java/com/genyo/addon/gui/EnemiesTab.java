package com.genyo.addon.gui;

import com.genyo.addon.systems.enemies.Enemies;
import com.genyo.addon.systems.enemies.Enemy;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;

public class EnemiesTab extends Tab {

    public EnemiesTab() {
        super("Enemies");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new EnemiesScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof EnemiesScreen;
    }

    private static class EnemiesScreen extends WindowTabScreen {
        private final Settings settings;

        public EnemiesScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            settings = Enemies.get().settings;
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();

            add(theme.settings(settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            initTable(table);

            // New
            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("", (text, c) -> c != ' ')).expandX().widget();
            nameW.setFocused(true);

            WPlus add = list.add(theme.plus()).widget();
            add.action = () -> {
                String name = nameW.get().trim();
                Enemy enemy = new Enemy(name);

                if (Enemies.get().add(enemy)) {
                    nameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        enemy.updateInfo();
                        reload();
                    });
                }
            };

            enterAction = add.action;
        }

        private void initTable(WTable table) {
            table.clear();
            if (Enemies.get().isEmpty()) return;

            Enemies.get().forEach(enemy ->
                MeteorExecutor.execute(() -> {
                    if (enemy.headTextureNeedsUpdate()) {
                        enemy.updateInfo();
                        reload();
                    }
                })
            );

            for (Enemy enemy : Enemies.get()) {
                table.add(theme.texture(32, 32, enemy.getHead().needsRotate() ? 90 : 0, enemy.getHead()));
                table.add(theme.label(enemy.getName()));

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    Enemies.get().remove(enemy);
                    reload();
                };

                table.row();
            }
        }

        @Override
        public void tick() {
            super.tick();

            settings.tick(window, theme);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Enemies.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Enemies.get());
        }
    }
}
