/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;
import org.lwjgl.PointerBuffer;

import java.io.File;
import java.util.function.Consumer;

public class FileSetting extends Setting<File> {
    public final PointerBuffer filters;

    public FileSetting(String name, String description, File defaultValue, Consumer<File> onChanged, Consumer<Setting<File>> onModuleActivated, IVisible visible, PointerBuffer filters) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filters = filters;
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
        private PointerBuffer filter;

        public Builder() {
            super(null);
        }

        public FileSetting.Builder filter(PointerBuffer filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public FileSetting build() {
            return new FileSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
