/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.function.Consumer;

public class FileSetting extends Setting<File> {
    /** SDL filter extensions, e.g. {@code "txt"}, {@code "txt;json"} or {@code "*"} */
    public final String filter;

    public FileSetting(String name, String description, File defaultValue, Consumer<File> onChanged, Consumer<Setting<File>> onModuleActivated, IVisible visible, String filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    protected File parseImpl(String str) {
        File file = new File(str);
        return file.exists() && file.isFile() ? file : null;
    }

    @Override
    protected boolean isValueValid(File value) {
        return value.exists() && value.isFile();
    }

    @Override
    protected CompoundTag save(CompoundTag tag) {
        if (get() != null) {
            tag.putString("file", get().getAbsolutePath());
        }

        return tag;
    }

    @Override
    protected File load(CompoundTag tag) {
        if (tag.contains("file")) {
            set(new File(tag.getStringOr("file", "")));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<FileSetting.Builder, File, FileSetting> {
        private String filter = "*";

        public Builder() {
            super(null);
        }

        public FileSetting.Builder filter(String filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public FileSetting build() {
            return new FileSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
