/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A simple in-game file browser.
 *
 * <p>The mod does not depend on a native file dialog library, so files are picked with this
 * screen instead. In save mode the callback receives the chosen name inside the current
 * directory, in open mode it receives the picked file.
 */
public class FileBrowserScreen extends WindowScreen {
    private final boolean save;
    private final List<String> filters;
    private final Consumer<File> callback;

    private File directory;
    private WLabel pathLabel;
    private WTable table;
    private WTextBox nameBox;

    public FileBrowserScreen(GuiTheme theme, String title, boolean save, List<String> filters, Consumer<File> callback) {
        super(theme, title);

        this.save = save;
        this.filters = filters;
        this.callback = callback;

        File home = new File(System.getProperty("user.home", "."));
        directory = home.isDirectory() ? home : new File(".");
    }

    @Override
    public void initWidgets() {
        pathLabel = add(theme.label("")).expandX().widget();

        table = add(theme.table()).expandX().minWidth(420).widget();

        add(theme.horizontalSeparator()).expandX();

        WHorizontalList list = add(theme.horizontalList()).expandX().widget();

        WButton up = list.add(theme.button("Up")).widget();
        up.action = () -> enter(directory.getParentFile());

        if (save) {
            nameBox = list.add(theme.textBox("")).minWidth(150).expandX().widget();
        }

        WButton cancel = list.add(theme.button("Cancel")).expandX().widget();
        cancel.action = this::onClose;

        if (save) {
            WButton saveButton = list.add(theme.button("Save")).widget();
            saveButton.action = () -> {
                String name = nameBox.get().trim();
                if (name.isEmpty()) return;

                callback.accept(new File(directory, name));
                onClose();
            };
        }

        refresh();
    }

    private void enter(File dir) {
        if (dir == null || !dir.isDirectory()) return;

        directory = dir;
        refresh();
    }

    private void refresh() {
        pathLabel.set(directory.getAbsolutePath());
        table.clear();

        File[] files = directory.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::isDirectory).reversed()
            .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File file : files) {
            if (file.isDirectory()) {
                WButton button = table.add(theme.button("[" + file.getName() + "]")).expandX().widget();
                button.action = () -> enter(file);
            } else if (matches(file)) {
                WButton button = table.add(theme.button(file.getName())).expandX().widget();
                button.action = () -> {
                    if (save) {
                        if (nameBox != null) nameBox.set(file.getName());
                    } else {
                        callback.accept(file);
                        onClose();
                    }
                };
            }
        }
    }

    /** Matches the file name against the filters, which are patterns like {@code *.txt}. */
    private boolean matches(File file) {
        if (filters == null || filters.isEmpty()) return true;

        String name = file.getName().toLowerCase();
        for (String filter : filters) {
            if (name.endsWith(filter.toLowerCase().replace("*", ""))) return true;
        }

        return false;
    }
}
