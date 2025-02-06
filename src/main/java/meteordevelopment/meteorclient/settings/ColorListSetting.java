/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.ColorArgumentType;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ColorListSetting extends Setting<List<SettingColor>> {
    public ColorListSetting(String name, String description, List<SettingColor> defaultValue, Consumer<List<SettingColor>> onChanged, Consumer<Setting<List<SettingColor>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("color", ColorArgumentType.color())
                .executes(context -> {
                    SettingColor color = ColorArgumentType.get(context, "color");
                    this.get().add(color);
                    output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", color, this.title));
                    this.onChanged();
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("color", new CollectionItemArgumentType<>(this::get))
                .executes(context -> {
                    SettingColor color = context.getArgument("color", SettingColor.class);
                    if (this.get().remove(color)) {
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", color, this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    protected void resetImpl() {
        value = new ArrayList<>(defaultValue.size());

        for (SettingColor settingColor : defaultValue) {
            value.add(new SettingColor(settingColor));
        }
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.put("value", NbtUtils.listToTag(get()));

        return tag;
    }

    @Override
    protected List<SettingColor> load(NbtCompound tag) {
        get().clear();

        for (NbtElement e : tag.getList("value", NbtElement.COMPOUND_TYPE)) {
            get().add(new SettingColor().fromTag((NbtCompound) e));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<SettingColor>, ColorListSetting> {
        public Builder() {
            super(new ArrayList<>());
        }

        @Override
        public ColorListSetting build() {
            return new ColorListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
