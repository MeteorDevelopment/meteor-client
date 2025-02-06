/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EntityTypeListSetting extends Setting<Set<EntityType<?>>> {
    public final Predicate<EntityType<?>> filter;
    private List<String> suggestions;
    private final static List<String> groups = List.of("animal", "wateranimal", "monster", "ambient", "misc");

    public EntityTypeListSetting(String name, String description, Set<EntityType<?>> defaultValue, Consumer<Set<EntityType<?>>> onChanged, Consumer<Setting<Set<EntityType<?>>>> onModuleActivated, IVisible visible, Predicate<EntityType<?>> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ObjectOpenHashSet<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("entity", RegistryEntryArgumentType.entityType())
                .executes(context -> {
                    RegistryEntry.Reference<EntityType<?>> entry = RegistryEntryArgumentType.getEntityType(context, "entity");
                    if ((filter == null || filter.test(entry.value()) && this.get().add(entry.value()))) {
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("entity", new CollectionItemArgumentType<>(this::get, Names::get))
                .executes(context -> {
                    EntityType<?> entityType = context.getArgument("entity", EntityType.class);
                    if (this.get().remove(entityType)) {
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(entityType), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public List<String> getSuggestions() {
        if (suggestions == null) {
            suggestions = new ArrayList<>(groups);
            for (EntityType<?> entityType : Registries.ENTITY_TYPE) {
                if (filter == null || filter.test(entityType)) suggestions.add(Registries.ENTITY_TYPE.getId(entityType).toString());
            }
        }

        return suggestions;
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (EntityType<?> entityType : get()) {
            valueTag.add(NbtString.of(Registries.ENTITY_TYPE.getId(entityType).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Set<EntityType<?>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(tagI.asString()));
            if (filter == null || filter.test(type)) get().add(type);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Set<EntityType<?>>, EntityTypeListSetting> {
        private Predicate<EntityType<?>> filter;

        public Builder() {
            super(new ObjectOpenHashSet<>(0));
        }

        public Builder defaultValue(EntityType<?>... defaults) {
            return defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>(0));
        }

        public Builder onlyAttackable() {
            filter = EntityUtils::isAttackable;
            return this;
        }

        public Builder filter(Predicate<EntityType<?>> filter){
            this.filter = filter;
            return this;
        }

        @Override
        public EntityTypeListSetting build() {
            return new EntityTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
