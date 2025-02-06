/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemSetting extends Setting<Item> {
    public final Predicate<Item> filter;

    public ItemSetting(String name, String description, Item defaultValue, Consumer<Item> onChanged, Consumer<Setting<Item>> onModuleActivated, IVisible visible, Predicate<Item> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("item", RegistryEntryArgumentType.item())
                .executes(context -> {
                    Item item = RegistryEntryArgumentType.getItem(context, "item").value();
                    if (this.set(item)) {
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", this.title, Names.get(item)));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    protected boolean isValueValid(Item value) {
        return filter == null || filter.test(value);
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.ITEM.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        tag.putString("value", Registries.ITEM.getId(get()).toString());

        return tag;
    }

    @Override
    public Item load(NbtCompound tag) {
        value = Registries.ITEM.get(Identifier.of(tag.getString("value")));

        if (filter != null && !filter.test(value)) {
            for (Item item : Registries.ITEM) {
                if (filter.test(item)) {
                    value = item;
                    break;
                }
            }
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Item, ItemSetting> {
        private Predicate<Item> filter;

        public Builder() {
            super(null);
        }

        public Builder filter(Predicate<Item> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public ItemSetting build() {
            return new ItemSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
