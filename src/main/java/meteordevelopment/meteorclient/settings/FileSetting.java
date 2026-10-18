/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.CompoundTag;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.util.function.Consumer;

public class FileSetting extends Setting<File> {
    public final SDL_DialogFileFilter.Buffer filters;

    public FileSetting(String name, String description, File defaultValue, Consumer<File> onChanged, Consumer<Setting<File>> onModuleActivated, IVisible visible, SDL_DialogFileFilter.Buffer filters) {
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
        private SDL_DialogFileFilter.Buffer filter;

        public Builder() {
            super(null);
        }

        public FileSetting.Builder filter(SDL_DialogFileFilter.Buffer filter) {
            this.filter = filter;
            return this;
        }

        public FileSetting.Builder filter(String... filters) {
            MemoryStack stack = MemoryStack.stackPush();
            filter = SDL_DialogFileFilter.malloc(filters.length, stack);
            for (int i = 0; i < filters.length; i++) {
                filter.get(i).name(stack.UTF8("Filter " + i)).pattern(stack.UTF8(filters[i]));
            }
            return this;
        }

        @Override
        public FileSetting build() {
            return new FileSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
