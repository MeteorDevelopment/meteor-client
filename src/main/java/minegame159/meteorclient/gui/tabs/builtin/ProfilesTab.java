/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.tabs.builtin;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.WindowTabScreen;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.gui.widgets.pressable.WCheckbox;
import minegame159.meteorclient.gui.widgets.pressable.WMinus;
import minegame159.meteorclient.gui.widgets.pressable.WPlus;
import minegame159.meteorclient.systems.accounts.Accounts;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.macros.Macros;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.profiles.Profile;
import minegame159.meteorclient.systems.profiles.Profiles;
import minegame159.meteorclient.systems.waypoints.Waypoints;
import net.minecraft.client.gui.screen.Screen;
import org.apache.commons.lang3.StringUtils;

import static minegame159.meteorclient.utils.Utils.mc;

public class ProfilesTab extends Tab {

    public ProfilesTab() {
        super("Profiles");
    }

    @Override
    protected TabScreen createScreen(GuiTheme theme) {
        return new ProfilesScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ProfilesScreen;
    }

    private static class ProfilesScreen extends WindowTabScreen {

        public ProfilesScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        protected void init() {
            super.init();

            initWidget();
        }

        private void initWidget() {
            clear();

            WTable table = add(theme.table()).expandX().minWidth(300).widget();

            // Waypoints
            for (Profile profile : Profiles.get()) {
                // Name
                table.add(theme.label(profile.name)).expandCellX();

                // Save
                WButton save = table.add(theme.button("Save")).widget();
                save.action = profile::save;

                // Load
                WButton load = table.add(theme.button("Load")).widget();
                load.action = profile::load;

                // Edit
                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
                edit.action = () -> mc.openScreen(new EditProfileScreen(theme, profile, this::initWidget));

                // Remove
                WMinus remove = table.add(theme.minus()).widget();
                remove.action = () -> {
                    Profiles.get().remove(profile);
                    initWidget();
                };

                table.row();
            }

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Create
            WButton create = table.add(theme.button("Create")).expandX().widget();
            create.action = () -> mc.openScreen(new EditProfileScreen(theme, null, this::initWidget));
        }
    }

    private static class EditProfileScreen extends WindowScreen {
        private final Profile profile;
        private final boolean newProfile;
        private final Runnable action;

        public EditProfileScreen(GuiTheme theme, Profile profile, Runnable action) {
            super(theme, profile == null ? "New Profile" : "Edit Profile");

            this.newProfile = profile == null;
            this.profile = newProfile ? new Profile() : profile;
            this.action = action;

            initWidgets();
        }

        public void initWidgets() {
            WTable table = add(theme.table()).expandX().widget();

            // Name
            table.add(theme.label("Name:"));
            WTextBox name = table.add(theme.textBox(newProfile ? "" : profile.name)).minWidth(400).expandX().widget();
            name.action = () -> profile.name = name.get().trim();
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // On Launch
            table.add(theme.label("Load on Launch:"));
            WCheckbox onLaunch = table.add(theme.checkbox(profile.onLaunch)).widget();
            onLaunch.action = () -> profile.onLaunch = onLaunch.checked;
            table.row();

            // On Server Join
            table.add(theme.label("Load when Joining:"));
            WTable ips = table.add(theme.table()).widget();
            fillTable(ips);
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Accounts
            table.add(theme.label("Accounts:"));
            WCheckbox accounts = table.add(theme.checkbox(profile.accounts)).widget();
            accounts.action = () -> {
                profile.accounts = accounts.checked;

                if (newProfile) return;
                if (profile.accounts) profile.save(Accounts.get());
                else profile.delete(Accounts.get());
            };
            table.row();

            // Config
            table.add(theme.label("Config:"));
            WCheckbox config = table.add(theme.checkbox(profile.config)).widget();
            config.action = () -> {
                profile.config = config.checked;

                if (newProfile) return;
                if (profile.config) profile.save(Config.get());
                else profile.delete(Config.get());
            };
            table.row();

            // Friends
            table.add(theme.label("Friends:"));
            WCheckbox friends = table.add(theme.checkbox(profile.friends)).widget();
            friends.action = () -> {
                profile.friends = friends.checked;

                if (newProfile) return;
                if (profile.friends) profile.save(Friends.get());
                else profile.delete(Friends.get());
            };
            table.row();

            // Macros
            table.add(theme.label("Macros:"));
            WCheckbox macros = table.add(theme.checkbox(profile.macros)).widget();
            macros.action = () -> {
                profile.macros = macros.checked;

                if (newProfile) return;
                if (profile.macros) profile.save(Macros.get());
                else profile.delete(Macros.get());
            };
            table.row();

            // Modules
            table.add(theme.label("Modules:"));
            WCheckbox modules = table.add(theme.checkbox(profile.modules)).widget();
            modules.action = () -> {
                profile.modules = modules.checked;

                if (newProfile) return;
                if (profile.modules) profile.save(Modules.get());
                else profile.delete(Modules.get());
            };
            table.row();

            // Waypoints
            table.add(theme.label("Waypoints:"));
            WCheckbox waypoints = table.add(theme.checkbox(profile.waypoints)).widget();
            waypoints.action = () -> {
                profile.waypoints = waypoints.checked;

                if (newProfile) return;
                if (profile.waypoints) profile.save(Waypoints.get());
                else profile.delete(Waypoints.get());
            };
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Save
            table.add(theme.button("Save")).expandX().widget().action = () -> {
                if (profile.name == null || profile.name.isEmpty()) return;

                for (Profile p : Profiles.get()) {
                    if (profile == p) continue;
                    if (profile.name.equalsIgnoreCase(p.name)) return;
                }

                if (newProfile) {
                    Profiles.get().add(profile);
                } else {
                    Profiles.get().save();
                }

                onClose();
            };
        }

        private void fillTable(WTable table) {
            if (profile.loadOnJoinIps.isEmpty()) profile.loadOnJoinIps.add("");

            for (int i = 0; i < profile.loadOnJoinIps.size(); i++) {
                int ii = i;

                WTextBox line = table.add(theme.textBox(profile.loadOnJoinIps.get(ii))).minWidth(400).expandX().widget();
                line.action = () -> {
                    String ip = line.get().trim();
                    if (StringUtils.containsWhitespace(ip) || !ip.contains(".")) return;

                    profile.loadOnJoinIps.set(ii, ip);
                };

                if (ii != profile.loadOnJoinIps.size() - 1) {
                    WMinus remove = table.add(theme.minus()).widget();
                    remove.action = () -> {
                        profile.loadOnJoinIps.remove(ii);

                        clear();
                        initWidgets();
                    };
                } else {
                    WPlus add = table.add(theme.plus()).widget();
                    add.action = () -> {
                        profile.loadOnJoinIps.add("");

                        clear();
                        initWidgets();
                    };
                }

                table.row();
            }
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }
}
