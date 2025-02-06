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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ParticleTypeListSetting extends Setting<List<ParticleType<?>>> {
    public ParticleTypeListSetting(String name, String description, List<ParticleType<?>> defaultValue, Consumer<List<ParticleType<?>>> onChanged, Consumer<Setting<List<ParticleType<?>>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("add")
            .then(Command.argument("particle", RegistryEntryArgumentType.particleType())
                .executes(context -> {
                    RegistryEntry<ParticleType<?>> entry = RegistryEntryArgumentType.getParticleType(context, "particle");
                    if (!this.get().contains(entry.value())) {
                        this.get().add(entry.value());
                        output.accept(String.format("Added (highlight)%s(default) to (highlight)%s(default).", Names.get(entry.value()), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("particle", new CollectionItemArgumentType<>(this::get, Names::get))
                .executes(context -> {
                    ParticleType<?> particleType = context.getArgument("particle", ParticleType.class);
                    if (this.get().remove(particleType)) {
                        this.onChanged();
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(particleType), this.title));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.PARTICLE_TYPE.getIds();
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (ParticleType<?> particleType : get()) {
            Identifier id = Registries.PARTICLE_TYPE.getId(particleType);
            if (id != null) valueTag.add(NbtString.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<ParticleType<?>> load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getList("value", 8);
        for (NbtElement tagI : valueTag) {
            ParticleType<?> particleType = Registries.PARTICLE_TYPE.get(Identifier.of(tagI.asString()));
            if (particleType != null) get().add(particleType);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<ParticleType<?>>, ParticleTypeListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(ParticleType<?>... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public ParticleTypeListSetting build() {
            return new ParticleTypeListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
