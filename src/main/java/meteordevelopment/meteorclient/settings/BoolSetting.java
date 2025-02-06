/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.function.Consumer;

public class BoolSetting extends Setting<Boolean> {
    private static final List<String> SUGGESTIONS = List.of("true", "false", "toggle");

    private BoolSetting(String name, String description, Boolean defaultValue, Consumer<Boolean> onChanged, Consumer<Setting<Boolean>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("toggle").executes(context -> {
            this.set(!this.get());
            output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, this.get()));
            return Command.SINGLE_SUCCESS;
        }));

        builder.then(Command.literal("set")
            .then(Command.argument("value", BoolArgumentType.bool())
                .executes(context -> {
                    this.set(BoolArgumentType.getBool(context, "value"));
                    output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, this.get()));
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public List<String> getSuggestions() {
        return SUGGESTIONS;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putBoolean("value", get());

        return tag;
    }

    @Override
    public Boolean load(NbtCompound tag) {
        set(tag.getBoolean("value"));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Boolean, BoolSetting> {
        public Builder() {
            super(false);
        }

        @Override
        public BoolSetting build() {
            return new BoolSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
