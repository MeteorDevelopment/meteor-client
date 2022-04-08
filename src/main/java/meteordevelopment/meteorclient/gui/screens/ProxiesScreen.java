/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.systems.proxies.ProxyType;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProxiesScreen extends WindowScreen {
    private final List<WCheckbox> checkboxes = new ArrayList<>();
    boolean dirty;

    public ProxiesScreen(GuiTheme theme) {
        super(theme, "Proxies");
    }

    protected void openEditProxyScreen(Proxy proxy) {
        mc.setScreen(new EditProxyScreen(theme, proxy));
    }

    @Override
    protected void init() {
        super.init();

        if (dirty) {
            reload();
            dirty = false;
        }
    }

    @Override
    public void initWidgets() {
        // Proxies
        WTable table = add(theme.table()).expandX().widget();

        for (Proxy proxy : Proxies.get()) {
            // Enabled
            WCheckbox enabled = table.add(theme.checkbox(proxy.enabled)).widget();
            checkboxes.add(enabled);
            enabled.action = () -> {
                boolean checked = enabled.checked;
                Proxies.get().setEnabled(proxy, checked);

                for (WCheckbox checkbox : checkboxes) checkbox.checked = false;
                enabled.checked = checked;
            };

            // Name
            WLabel name = table.add(theme.label(proxy.name)).widget();
            name.color = theme.textColor();

            // Type
            WLabel type = table.add(theme.label("(" + proxy.type + ")")).widget();
            type.color = theme.textSecondaryColor();

            // IP + Port
            WHorizontalList ipList = table.add(theme.horizontalList()).expandCellX().widget();
            ipList.spacing = 0;

            ipList.add(theme.label(proxy.address));
            ipList.add(theme.label(":")).widget().color = theme.textSecondaryColor();
            ipList.add(theme.label(Integer.toString(proxy.port)));

            // Edit
            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> openEditProxyScreen(proxy);

            // Remove
            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Proxies.get().remove(proxy);
                reload();
            };

            table.row();
        }

        WHorizontalList l = add(theme.horizontalList()).expandX().widget();

        // New
        WButton newBtn = l.add(theme.button("New")).expandX().widget();
        newBtn.action = () -> openEditProxyScreen(null);

        // Import
        PointerBuffer filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");

        filters.put(txtFilter);
        filters.rewind();

        WButton importBtn = l.add(theme.button("Import")).expandX().widget();
        importBtn.action = () -> {
            String selectedFile = TinyFileDialogs.tinyfd_openFileDialog("Import Proxies", null, filters, null, false);
            if (selectedFile != null) {
                File file = new File(selectedFile);
                mc.setScreen(new ProxiesImportScreen(theme, file));
            }
        };
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Proxies.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Proxies.get());
    }

    protected class EditProxyScreen extends WindowScreen {
        private final boolean isNew;
        private final Proxy proxy;

        public EditProxyScreen(GuiTheme theme, Proxy p) {
            super(theme, p == null ? "New Proxy" : "Edit Proxy");

            isNew = p == null;
            proxy = isNew ? new Proxy() : p;
        }

        @Override
        public void initWidgets() {
            // General
            WTable general = add(theme.table()).expandX().widget();

            //   Name
            general.add(theme.label("Proxy Name:"));
            WTextBox name = general.add(theme.textBox(proxy.name)).expandX().widget();
            name.action = () -> proxy.name = name.get();
            general.row();

            //   Type
            general.add(theme.label("Type:"));
            WDropdown<ProxyType> type = general.add(theme.dropdown(proxy.type)).widget();
            type.action = () -> proxy.type = type.get();
            general.row();

            //   IP
            general.add(theme.label("IP:"));
            WTextBox ip = general.add(theme.textBox(proxy.address)).minWidth(400).expandX().widget();
            ip.action = () -> proxy.address = ip.get();
            general.row();

            //   Port
            general.add(theme.label("Port:"));
            WIntEdit port = general.add(theme.intEdit(proxy.port, 0, 65535, true)).expandX().widget();
            port.action = () -> proxy.port = port.get();

            // Optional
            add(theme.horizontalSeparator("Optional")).expandX().widget();
            WTable optional = add(theme.table()).expandX().widget();

            //   Username
            optional.add(theme.label("Username:"));
            WTextBox username = optional.add(theme.textBox(proxy.username)).expandX().widget();
            username.action = () -> proxy.username = username.get();
            optional.row();

            //   Password
            optional.add(theme.label("Password:"));
            WTextBox password = optional.add(theme.textBox(proxy.password)).expandX().widget();
            password.action = () -> proxy.password = password.get();

            // Add / Save
            add(theme.horizontalSeparator()).expandX();

            WButton addSave = add(theme.button(isNew ? "Add" : "Save")).expandX().widget();
            addSave.action = () -> {
                if (proxy.resolveAddress() && (!isNew || Proxies.get().add(proxy))) {
                    dirty = true;
                    close();
                }
            };

            enterAction = addSave.action;
        }
    }
}
