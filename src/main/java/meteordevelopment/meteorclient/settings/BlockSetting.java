/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockSetting extends Setting<Block> {
    public final Predicate<Block> filter;

    public BlockSetting(String name, String description, Block defaultValue, Consumer<Block> onChanged, Consumer<Setting<Block>> onModuleActivated, IVisible visible, Predicate<Block> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("block", RegistryEntryArgumentType.block())
                .executes(context -> {
                    RegistryEntry<Block> blockEntry = RegistryEntryArgumentType.getBlock(context, "block");
                    if (isValueValid(blockEntry.value())) {
                        this.set(blockEntry.value());
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, blockEntry.getIdAsString()));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    protected boolean isValueValid(Block value) {
        return filter == null || filter.test(value);
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.BLOCK.getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putString("value", Registries.BLOCK.getId(get()).toString());

        return tag;
    }

    @Override
    protected Block load(NbtCompound tag) {
        value = Registries.BLOCK.get(Identifier.of(tag.getString("value")));

        if (filter != null && !filter.test(value)) {
            for (Block block : Registries.BLOCK) {
                if (filter.test(block)) {
                    value = block;
                    break;
                }
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Block, BlockSetting> {
        private Predicate<Block> filter;

        public Builder() {
            super(null);
        }

        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public BlockSetting build() {
            return new BlockSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
