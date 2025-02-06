/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockListSetting extends Setting<List<Block>> {
    public final Predicate<Block> filter;

    public BlockListSetting(String name, String description, List<Block> defaultValue, Consumer<List<Block>> onChanged, Consumer<Setting<List<Block>>> onModuleActivated, Predicate<Block> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("remove")
            .then(Command.argument("block", new CollectionItemArgumentType<>(this::get, t -> Registries.BLOCK.getId(t).toString()))
                .executes(context -> {
                    Block block = context.getArgument("block", Block.class);
                    if (this.get().remove(block)) {
                        String blockName = Registries.BLOCK.getId(block).toString();
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", blockName, this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("add")
            .then(Command.argument("block", RegistryEntryArgumentType.block()).executes(context -> {
                Block block = RegistryEntryArgumentType.getBlock(context, "block").value();
                String blockName = Registries.BLOCK.getId(block).toString();
                if ((filter == null || filter.test(block)) && !this.get().contains(block)) {
                    this.get().add(block);
                    output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", blockName, this.title));
                    this.onChanged();
                } else {
                    output.accept(String.format("Could not add (highlight)%s(default) to (highlight)%s(default).", blockName, this.title));
                }
                return Command.SINGLE_SUCCESS;
            }))
        );
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.BLOCK.getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Block block : get()) {
            valueTag.add(NbtString.of(Registries.BLOCK.getId(block).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    protected List<Block> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            Block block = Registries.BLOCK.get(Identifier.of(tagI.asString()));

            if (filter == null || filter.test(block)) get().add(block);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Block>, BlockListSetting> {
        private Predicate<Block> filter;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(Block... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public BlockListSetting build() {
            return new BlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
