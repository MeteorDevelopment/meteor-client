/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ColorArgumentType;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.function.Consumer;

public class ColorSetting extends Setting<SettingColor> {
    public static final List<String> SUGGESTIONS = List.of("0 0 0 255", "225 25 25 255", "25 225 25 255", "25 25 225 255", "255 255 255 255");

    public ColorSetting(String name, String description, SettingColor defaultValue, Consumer<SettingColor> onChanged, Consumer<Setting<SettingColor>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("color", ColorArgumentType.color())
                .executes(context -> {
                    SettingColor color = ColorArgumentType.get(context, "color");
                    this.set(color);
                    output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, this.get()));
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public void resetImpl() {
        if (value == null) value = new SettingColor(defaultValue);
        else value.set(defaultValue);
    }

    @Override
    protected boolean isValueValid(SettingColor value) {
        value.validate();

        return true;
    }

    @Override
    public List<String> getSuggestions() {
        return SUGGESTIONS;
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.put("value", get().toTag());

        return tag;
    }

    @Override
    public SettingColor load(NbtCompound tag) {
        get().fromTag(tag.getCompound("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, SettingColor, ColorSetting> {
        public Builder() {
            super(new SettingColor());
        }

        @Override
        public ColorSetting build() {
            return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }

        @Override
        public Builder defaultValue(SettingColor defaultValue) {
            this.defaultValue.set(defaultValue);
            return this;
        }

        public Builder defaultValue(Color defaultValue) {
            this.defaultValue.set(defaultValue);
            return this;
        }
    }
}
