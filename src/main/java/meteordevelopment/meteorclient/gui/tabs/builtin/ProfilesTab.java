/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screen.Screen;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProfilesTab extends Tab {

    public ProfilesTab() {
        super("Profiles");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
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
        public void initWidgets() {
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
                edit.action = () -> mc.setScreen(new EditProfileScreen(theme, profile, this::reload));

                // Remove
                WMinus remove = table.add(theme.minus()).widget();
                remove.action = () -> {
                    Profiles.get().remove(profile);
                    reload();
                };

                table.row();
            }

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Create
            WButton create = table.add(theme.button("Create")).expandX().widget();
            create.action = () -> mc.setScreen(new EditProfileScreen(theme, null, this::reload));
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Profiles.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Profiles.get());
        }
    }

    private static class EditProfileScreen extends WindowScreen {
        private final Profile newProfile;
        private final Profile oldProfile;
        private final boolean isNew;
        private final Runnable action;

        public EditProfileScreen(GuiTheme theme, Profile profile, Runnable action) {
            super(theme, profile == null ? "New Profile" : "Edit Profile");

            this.isNew = profile == null;
            this.newProfile = new Profile();
            this.oldProfile = isNew ? new Profile() : profile;
            this.action = action;

            newProfile.set(oldProfile);
        }

        @Override
        public void initWidgets() {
            initWidgets(oldProfile, newProfile.loadOnJoinIps);
        }

        public void initWidgets(Profile ogProfile, List<String> list) {
            WTable table = add(theme.table()).expandX().widget();

            // Name
            table.add(theme.label("Name:"));
            WTextBox nameInput = table.add(theme.textBox(ogProfile.name, this::nameFilter)).minWidth(400).expandX().widget();
            nameInput.action = () -> newProfile.name = nameInput.get();
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // On Launch
            table.add(theme.label("Load on Launch:"));
            WCheckbox onLaunchCheckbox = table.add(theme.checkbox(ogProfile.onLaunch)).widget();
            onLaunchCheckbox.action = () -> newProfile.onLaunch = onLaunchCheckbox.checked;
            table.row();

            // On Server Join
            table.add(theme.label("Load when Joining:"));
            WTable ips = table.add(theme.table()).widget();
            initTable(ips, list);
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Accounts
            table.add(theme.label("Accounts:"));
            WCheckbox accountsBool = table.add(theme.checkbox(ogProfile.accounts)).widget();
            accountsBool.action = () -> newProfile.accounts = accountsBool.checked;
            table.row();

            // Config
            table.add(theme.label("Config:"));
            WCheckbox configBool = table.add(theme.checkbox(ogProfile.config)).widget();
            configBool.action = () -> newProfile.config = configBool.checked;
            table.row();

            // Friends
            table.add(theme.label("Friends:"));
            WCheckbox friendsBool = table.add(theme.checkbox(ogProfile.friends)).widget();
            friendsBool.action = () -> newProfile.friends = friendsBool.checked;
            table.row();

            // Macros
            table.add(theme.label("Macros:"));
            WCheckbox macrosBool = table.add(theme.checkbox(ogProfile.macros)).widget();
            macrosBool.action = () -> newProfile.macros = macrosBool.checked;
            table.row();

            // Modules
            table.add(theme.label("Modules:"));
            WCheckbox modulesBool = table.add(theme.checkbox(ogProfile.modules)).widget();
            modulesBool.action = () -> newProfile.modules = modulesBool.checked;
            table.row();

            // Waypoints
            table.add(theme.label("Waypoints:"));
            WCheckbox waypointsBool = table.add(theme.checkbox(ogProfile.waypoints)).widget();
            waypointsBool.action = () -> newProfile.waypoints = waypointsBool.checked;
            table.row();

            // HUD
            table.add(theme.label("HUD:"));
            WCheckbox hudBool = table.add(theme.checkbox(ogProfile.hud)).widget();
            hudBool.action = () -> newProfile.hud = hudBool.checked;
            table.row();

            table.add(theme.horizontalSeparator()).expandX();
            table.row();

            // Save
            WButton save = table.add(theme.button("Save")).expandX().widget();
            save.action = () -> {
                if (newProfile.name.isEmpty()) return;

                for (Profile p : Profiles.get()) {
                    if (newProfile.equals(p) && !oldProfile.equals(p)) return;
                }

                oldProfile.set(newProfile);

                if (isNew) {
                    Profiles.get().add(oldProfile);
                } else {
                    Profiles.get().save();
                }

                onClose();
            };

            enterAction = save.action;
        }

        private void initTable(WTable table, List<String> ipList) {
            if (ipList.isEmpty()) ipList.add("");

            for (int i = 0; i < ipList.size(); i++) {
                int ii = i;

                WTextBox line = table.add(theme.textBox(ipList.get(ii), this::ipFilter)).minWidth(400).expandX().widget();
                line.action = () -> {
                    String ip = line.get().trim();

                    if (!ip.contains(".") || StringUtils.containsWhitespace(ip)) return;

                    ipList.set(ii, ip);
                };

                if (ii != ipList.size() - 1) {
                    WMinus remove = table.add(theme.minus()).widget();
                    remove.action = () -> {
                        ipList.remove(ii);

                        clear();
                        initWidgets(newProfile, ipList);
                    };
                } else {
                    WPlus add = table.add(theme.plus()).widget();
                    add.action = () -> {
                        ipList.add("");

                        clear();
                        initWidgets(newProfile, ipList);
                    };
                }

                table.row();
            }
        }

        private boolean nameFilter(String text, char character) {
            return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '_' || character == '-' || character == '.' || character == ' ';
        }

        private boolean ipFilter(String text, char character) {
            if (text.contains(":") && character == ':') return false;
            return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '.';
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }
}
