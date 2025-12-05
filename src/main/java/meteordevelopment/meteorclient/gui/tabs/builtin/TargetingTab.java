/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.targeting.SavedPlayer;
import meteordevelopment.meteorclient.systems.targeting.Targeting;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TargetingTab extends Tab {
    public TargetingTab() {
        super("Players");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new TargetingScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof TargetingScreen;
    }

    private static class TargetingScreen extends WindowTabScreen {
        public TargetingScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable outerTable = add(theme.table()).expandX().minWidth(800).widget();
            WTable friends = outerTable.add(theme.table()).expandX().minWidth(400).top().widget();
            outerTable.add(theme.verticalSeparator());
            WTable enemies = outerTable.add(theme.table()).expandX().minWidth(400).top().widget();

            Targeting targeting = Targeting.get();

            initTable(friends, "Friends", targeting.getFriends(), targeting::removeFriend);
            initTable(enemies, "Enemies", targeting.getEnemies(), targeting::removeEnemy);

            add(theme.horizontalSeparator()).expandX();

            // New
            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("", (text, c) -> c != ' ')).expandX().widget();
            nameW.setFocused(true);

            WButton addF = list.add(theme.button("Friend")).widget();
            WButton addE = list.add(theme.button("Enemy")).widget();

            addF.action = () -> {
                String name = nameW.get().trim();
                SavedPlayer friend = new SavedPlayer(name);

                if (targeting.addFriend(friend)) {
                    nameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        friend.updateInfo();
                        mc.execute(this::reload);
                    });
                }
            };

            addE.action = () -> {
                String name = nameW.get().trim();
                SavedPlayer enemy = new SavedPlayer(name);

                if (targeting.addEnemy(enemy)) {
                    nameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        enemy.updateInfo();
                        mc.execute(this::reload);
                    });
                }
            };
        }

        private void initTable(WTable table, String title,Iterable<SavedPlayer> source, Consumer<SavedPlayer> removeAction) {
            table.clear();

            table.add(theme.label(title, true)).expandCellX().centerX();

            table.row();

            source.forEach(friend ->
                MeteorExecutor.execute(() -> {
                    if (friend.headTextureNeedsUpdate()) {
                        friend.updateInfo();
                    }
                })
            );

            for (SavedPlayer friend : source) {
                table.add(theme.texture(32, 32, friend.getHead().needsRotate() ? 90 : 0, friend.getHead()));
                table.add(theme.label(friend.getName()));

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    removeAction.accept(friend);
                    reload();
                };

                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Targeting.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Targeting.get());
        }
    }
}
