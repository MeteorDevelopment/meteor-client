/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends Setting<List<Item>> {
    public final Predicate<Item> filter;
    private final boolean bypassFilterWhenSavingAndLoading;

    public ItemListSetting(String name, String description, List<Item> defaultValue, Consumer<List<Item>> onChanged, Consumer<Setting<List<Item>>> onModuleActivated, IVisible visible, Predicate<Item> filter, boolean bypassFilterWhenSavingAndLoading) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
        this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("item", RegistryEntryArgumentType.item())
                .executes(context -> {
                    RegistryEntry<Item> entry = RegistryEntryArgumentType.getItem(context, "item");
                    if ((filter == null || filter.test(entry.value())) && !this.get().contains(entry.value())) {
                        this.get().add(entry.value());
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("item", new CollectionItemArgumentType<>(this::get, Names::get))
                .executes(context -> {
                    Item item = context.getArgument("item", Item.class);
                    if (this.get().remove(item)) {
                        this.onChanged();
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(item), this.title));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.ITEM.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (Item item : get()) {
            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) valueTag.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Item> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            Item item = Registries.ITEM.get(Identifier.of(tagI.asString()));

            if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) get().add(item);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<Item>, ItemListSetting> {
        private Predicate<Item> filter;
        private boolean bypassFilterWhenSavingAndLoading;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(Item... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        public Builder bypassFilterWhenSavingAndLoading() {
            this.bypassFilterWhenSavingAndLoading = true;
            return this;
        }

        @Override
        public ItemListSetting build() {
            return new ItemListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, bypassFilterWhenSavingAndLoading);
        }
    }
}
