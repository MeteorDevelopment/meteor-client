/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

public class StringSetting extends Setting<String> {
    public final Class<? extends WTextBox.Renderer> renderer;
    public final CharFilter filter;
    public final boolean wide;

    public StringSetting(String name, String description, String defaultValue, Consumer<String> onChanged, Consumer<Setting<String>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer, CharFilter filter, boolean wide) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.renderer = renderer;
        this.filter = filter;
        this.wide = wide;
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("string", StringArgumentType.string())
                .executes(context -> {
                    if (this.set(StringArgumentType.getString(context, "string"))) {
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, this.get()));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putString("value", get());

        return tag;
    }

    @Override
    public String load(NbtCompound tag) {
        set(tag.getString("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, String, StringSetting> {
        private Class<? extends WTextBox.Renderer> renderer;
        private CharFilter filter;
        private boolean wide;

        public Builder() {
            super("");
        }

        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder filter(CharFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder wide() {
            wide = true;
            return this;
        }

        @Override
        public StringSetting build() {
            return new StringSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer, filter, wide);
        }
    }
}
