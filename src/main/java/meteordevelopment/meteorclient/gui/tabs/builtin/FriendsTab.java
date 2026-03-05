/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.gui.screen.Screen;
import meteordevelopment.meteorclient.gui.widgets.WAccount;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FriendsTab extends Tab {

    private static final Logger log = LoggerFactory.getLogger(FriendsTab.class);

    public FriendsTab() {
        super("Friends");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new FriendsScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof FriendsScreen;
    }

    private static class FriendsScreen extends WindowTabScreen {
        public FriendsScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            // New
            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WTextBox nameW = list.add(theme.textBox("", (text, c) -> c != ' ')).expandX().widget();
            nameW.setFocused(true);

            WPlus add = list.add(theme.plus()).widget();
            add.action = () -> {
                String name = nameW.get().trim();
                Friend friend = new Friend(name);

                if (Friends.get().add(friend)) {
                    nameW.set("");
                    reload();

                    MeteorExecutor.execute(() -> {
                        friend.updateInfo();
                        mc.execute(this::reload);
                    });
                }
            };

            WButton list_players = list.add(theme.button("Nearby Players")).widget();
            list_players.action = () -> {
                if (mc.world != null) {
                    for (Entity entity : mc.world.getEntities()) {
                        EntityType<?> type = entity.getType();

                        //Only show players
                        if (type == EntityType.PLAYER) {
                            if (entity == mc.player) continue;
                            if (EntityUtils.getGameMode((PlayerEntity) entity) == null) continue;
                            if (Friends.get().isFriend((PlayerEntity) entity)) continue;
                        } else {
                            continue;
                        }
                        String name = entity.getNameForScoreboard();

                        //Display all rendered players
                        table.add(theme.label(name));
                        WPlus addAsFriend = table.add(theme.plus()).expandCellX().right().widget();
                        addAsFriend.action = () -> {
                            Friend friend = new Friend(entity.getStringifiedName());

                            if (Friends.get().add(friend)) {
                                nameW.set("");
                                reload();
                                //Add them as Friend
                                MeteorExecutor.execute(() -> {
                                    friend.updateInfo();
                                    mc.execute(this::reload);
                                });
                            }
                        };
                        table.row();

                    };
                }
            };



            enterAction = add.action;
        }

        private void initTable(WTable table) {
            table.clear();
            if (Friends.get().isEmpty()) return;

            Friends.get().forEach(friend ->
                MeteorExecutor.execute(() -> {
                    if (friend.headTextureNeedsUpdate()) {
                        friend.updateInfo();
                    }
                })
            );

            for (Friend friend : Friends.get()) {
                table.add(theme.texture(32, 32, friend.getHead().needsRotate() ? 90 : 0, friend.getHead()));
                table.add(theme.label(friend.getName()));

                WMinus remove = table.add(theme.minus()).expandCellX().right().widget();
                remove.action = () -> {
                    Friends.get().remove(friend);
                    reload();
                };

                table.row();
            }
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Friends.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Friends.get());
        }
    }
}
