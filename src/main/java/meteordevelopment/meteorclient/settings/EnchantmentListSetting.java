/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryReferenceArgumentType;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EnchantmentListSetting extends Setting<Set<RegistryKey<Enchantment>>> {
    public EnchantmentListSetting(String name, String description, Set<RegistryKey<Enchantment>> defaultValue, Consumer<Set<RegistryKey<Enchantment>>> onChanged, Consumer<Setting<Set<RegistryKey<Enchantment>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("enchantment", RegistryEntryReferenceArgumentType.enchantment())
                .executes(context -> {
                    RegistryEntry<Enchantment> entry = RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment");
                    if (this.get().add(entry.getKey().orElseThrow())) {
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("enchantment", new CollectionItemArgumentType<>(this::get, Names::get))
                .executes(context -> {
                    RegistryKey<Enchantment> entry = context.getArgument("enchantment", RegistryKey.class);
                    if (this.get().remove(entry)) {
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(entry), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Optional.ofNullable(MinecraftClient.getInstance().getNetworkHandler())
            .flatMap(networkHandler -> networkHandler.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT))
            .map(Registry::getIds).orElse(Set.of());
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (RegistryKey<Enchantment> ench : get()) {
            valueTag.add(NbtString.of(ench.getValue().toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<RegistryKey<Enchantment>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            get().add(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(tagI.asString())));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<RegistryKey<Enchantment>>, EnchantmentListSetting> {
        private static final Set<RegistryKey<Enchantment>> VANILLA_DEFAULTS;

        public Builder() {
            super(new ObjectOpenHashSet<>());
        }

        public Builder vanillaDefaults() {
            return defaultValue(VANILLA_DEFAULTS);
        }

        @SafeVarargs
        public final Builder defaultValue(RegistryKey<Enchantment>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>());
        }

        @Override
        public EnchantmentListSetting build() {
            return new EnchantmentListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }

        static {
            //noinspection unchecked,rawtypes
            VANILLA_DEFAULTS = (Set) Arrays.stream(Enchantments.class.getDeclaredFields())
                .filter(field -> field.accessFlags().containsAll(List.of(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL)))
                .filter(field -> field.getType() == RegistryKey.class)
                .map(field -> {
                    try {
                        return field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .map(RegistryKey.class::cast)
                .filter(registryKey -> registryKey.getRegistryRef() == RegistryKeys.ENCHANTMENT)
                .collect(Collectors.toSet());
        }
    }
}
