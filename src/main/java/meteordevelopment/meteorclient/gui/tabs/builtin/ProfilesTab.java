/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProfilesTab extends Tab {
    private static final PointerBuffer filters;

    static {
        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer pngFilter = MemoryUtil.memASCII("*.nbt");

        filters.put(pngFilter);
        filters.rewind();
    }

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
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList l = add(theme.horizontalList()).expandX().widget();

            // Create
            WButton create = l.add(theme.button("Create")).expandX().widget();
            create.tooltip = "Create new profile";
            create.action = () -> mc.setScreen(new EditProfileScreen(theme, null, this::reload));

            // Import
            WButton importBtn = l.add(theme.button("Import")).expandX().widget();
            importBtn.tooltip = "Import profile";
            importBtn.action = () -> {
                try {
                    Profile imported = importProfile();
                    if (imported != null) MeteorClient.LOG.info("Successfully imported profile '{}'.", imported.name.get());
                    reload();
                } catch (IOException e) {
                    MeteorClient.LOG.error("Error importing profile", e);
                    OkPrompt.create()
                        .title("Failure importing profile")
                        .message("There was an error importing the profile.")
                        .message("Error: %d", e.getMessage())
                        .dontShowAgainCheckboxVisible(false)
                        .show();
                }
            };
        }

        private void initTable(WTable table) {
            table.clear();
            if (Profiles.get().isEmpty()) return;

            for (Profile profile : Profiles.get()) {
                table.add(theme.label(profile.name.get())).expandCellX();

                WConfirmedButton save = theme.confirmedButton("Save", "Confirm");
                save.action = profile::save;
                table.add(save).right();

                WButton load = table.add(theme.button("Load")).widget();
                load.action = profile::load;

                WButton export = table.add(theme.button("Export")).widget();
                export.action = () -> mc.setScreen(new ExportProfileScreen(theme, profile));

                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
                edit.action = () -> mc.setScreen(new EditProfileScreen(theme, profile, this::reload));

                WConfirmedMinus remove = table.add(theme.confirmedMinus()).widget();
                remove.action = () -> {
                    Profiles.get().remove(profile);
                    reload();
                };

                table.row();
            }
        }

        private Profile importProfile() throws IOException {
            String file = TinyFileDialogs.tinyfd_openFileDialog("Select profile to import", null, filters, null, false);
            if (file == null) return null;
            File profileFile = new File(file);

            NbtCompound nbt = NbtIo.read(profileFile.toPath());

            Profile p = new Profile();
            p.name.set(nbt.getString("name", profileFile.getName()));
            //noinspection ResultOfMethodCallIgnored
            p.getFile().mkdirs();

            nbt.remove("name");
            for (Map.Entry<String, NbtElement> entry : nbt.entrySet()) {
                String filename = entry.getKey();

                switch (filename) {
                    case "hud.nbt" -> p.hud.set(true);
                    case "macros.nbt" -> p.macros.set(true);
                    case "modules.nbt" -> p.modules.set(true);
                    default -> {
                        if (filename.endsWith(".nbt")) p.waypoints.set(true);
                    }
                }

                File f = new File(p.getFile(), filename);
                NbtIo.write(entry.getValue(), new DataOutputStream(new FileOutputStream(f)));
            }

            Profiles.get().getAll().add(p);
            Profiles.get().save();

            return p;
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
        private WContainer settingsContainer;
        private final Profile profile;
        private final boolean isNew;
        private final Runnable action;

        public EditProfileScreen(GuiTheme theme, Profile profile, Runnable action) {
            super(theme, profile == null ? "New Profile" : "Edit Profile");

            this.isNew = profile == null;
            this.profile = isNew ? new Profile() : profile;
            this.action = action;
        }

        @Override
        public void initWidgets() {
            settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();
            settingsContainer.add(theme.settings(profile.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            WButton save = add(theme.button(isNew ? "Create" : "Save")).expandX().widget();
            save.action = () -> {
                if (profile.name.get().isEmpty()) return;

                if (isNew) {
                    for (Profile p : Profiles.get()) {
                        if (profile.equals(p)) return;
                    }
                }

                List<String> valid = new ArrayList<>();
                for (String address : profile.loadOnJoin.get()) {
                    if (Utils.resolveAddress(address)) valid.add(address);
                }

                profile.loadOnJoin.set(valid);

                if (isNew) Profiles.get().add(profile);
                else Profiles.get().save();

                close();
            };

            enterAction = save.action;
        }

        @Override
        public void tick() {
            profile.settings.tick(settingsContainer, theme);
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }

    private static class ExportProfileScreen extends WindowScreen {
        private final Profile profile;

        public ExportProfileScreen(GuiTheme theme, Profile profile) {
            super(theme, "Export Profile");
            this.profile = profile;
        }

        @Override
        public void initWidgets() {
            add(theme.label("Select which profile settings to export."));

            WContainer settingsContainer = add(theme.verticalList()).expandX().minWidth(400).widget();

            settingsContainer.add(theme.horizontalSeparator()).expandX().widget();

            WCheckbox hud = addBool(settingsContainer, profile.settings.get("hud", Boolean.class));
            WCheckbox macros = addBool(settingsContainer, profile.settings.get("macros", Boolean.class));
            WCheckbox modules = addBool(settingsContainer, profile.settings.get("modules", Boolean.class));
            WCheckbox waypoints = addBool(settingsContainer, profile.settings.get("waypoints", Boolean.class));

            add(theme.horizontalSeparator()).expandX().widget();

            WButton export = add(theme.button("Export profile")).expandX().widget();
            export.action = () -> {
                exportProfile(profile, hud.checked, macros.checked, modules.checked, waypoints.checked);
                close();
            };
        }

        private WCheckbox addBool(WContainer container, Setting<Boolean> setting) {
            WHorizontalList boolList = container.add(theme.horizontalList()).expandX().widget();
            boolList.add(theme.label(setting.title)).widget().tooltip = setting.description;

            WCheckbox c = theme.checkbox(setting.get());
            boolList.add(c).expandCellX().right();

            return c;
        }

        private void exportProfile(Profile profile, boolean hud, boolean macros, boolean modules, boolean waypoints) {
            String path = TinyFileDialogs.tinyfd_saveFileDialog("Save profile", profile.name.get(), filters, null);
            if (path == null) return;
            Path p = Path.of(path.endsWith(".nbt") ? path : path + ".nbt");

            NbtCompound nbt = new NbtCompound();
            nbt.putString("name", profile.name.get());

            try {
                for (File f : profile.getFile().listFiles()) {
                    if (f.getName().equals("hud.nbt") && hud ||
                        f.getName().equals("macros.nbt") && macros ||
                        f.getName().equals("modules.nbt") && modules
                    ) {
                        nbt.put(f.getName(), NbtIo.read(f.toPath()));
                    }
                    else if (f.getName().endsWith(".nbt") && waypoints)
                        nbt.put(f.getName(), NbtIo.read(f.toPath()));
                }

                NbtIo.write(nbt, p);
            } catch (IOException e) {
                MeteorClient.LOG.error("Error serialising profile {} to a file", profile.name.get(), e);
                OkPrompt.create()
                    .title("Failure exporting profile")
                    .message("There was an error serialising or exporting the profile %d.", profile.name.get())
                    .message("Error: %d", e.getMessage())
                    .dontShowAgainCheckboxVisible(false)
                    .show();
            }
        }
    }
}
