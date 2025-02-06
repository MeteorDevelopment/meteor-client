/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.CollectionItemArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryArgumentType;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class StatusEffectAmplifierMapSetting extends Setting<Reference2IntMap<StatusEffect>> {
    public static final Reference2IntMap<StatusEffect> EMPTY_STATUS_EFFECT_MAP = createStatusEffectMap();

    public StatusEffectAmplifierMapSetting(String name, String description, Reference2IntMap<StatusEffect> defaultValue, Consumer<Reference2IntMap<StatusEffect>> onChanged, Consumer<Setting<Reference2IntMap<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    public void resetImpl() {
        value = new Reference2IntOpenHashMap<>(defaultValue);
    }

    @Override
    public void buildCommandNode(LiteralArgumentBuilder<CommandSource> builder, Consumer<String> output) {
        builder.then(Command.literal("set")
            .then(Command.argument("effect", RegistryEntryArgumentType.statusEffect())
                .then(Command.argument("amplifier", IntegerArgumentType.integer(0))
                    .executes(context -> {
                        StatusEffect effect = RegistryEntryArgumentType.getStatusEffect(context, "effect").value();
                        int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                        this.get().put(effect, amplifier);
                        output.accept(String.format("Set (highlight)%s(default) to (highlight)%s(default).", Names.get(effect), amplifier));
                        this.onChanged();
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
        );

        builder.then(Command.literal("remove")
            .then(Command.argument("effect", new CollectionItemArgumentType<>(() -> this.get().keySet(), Names::get))
                .executes(context -> {
                    StatusEffect effect = context.getArgument("effect", StatusEffect.class);
                    if (this.get().containsKey(effect)) {
                        this.get().removeInt(effect);
                        output.accept(String.format("Removed (highlight)%s(default) from (highlight)%s(default).", Names.get(effect), this.title));
                        this.onChanged();
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
        );
    }

    @Override
    public NbtCompound save(NbtCompound tag) {
        NbtCompound valueTag = new NbtCompound();
        for (StatusEffect statusEffect : get().keySet()) {
            Identifier id = Registries.STATUS_EFFECT.getId(statusEffect);
            if (id != null) valueTag.putInt(id.toString(), get().getInt(statusEffect));
        }
        tag.put("value", valueTag);

        return tag;
    }

    private static Reference2IntMap<StatusEffect> createStatusEffectMap() {
        Reference2IntMap<StatusEffect> map = new Reference2IntArrayMap<>(Registries.STATUS_EFFECT.getIds().size());

        Registries.STATUS_EFFECT.forEach(potion -> map.put(potion, 0));

        return map;
    }

    @Override
    public Reference2IntMap<StatusEffect> load(NbtCompound tag) {
        get().clear();

        NbtCompound valueTag = tag.getCompound("value");
        for (String key : valueTag.getKeys()) {
            StatusEffect statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(key));
            if (statusEffect != null) get().put(statusEffect, valueTag.getInt(key));
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, Reference2IntMap<StatusEffect>, StatusEffectAmplifierMapSetting> {
        public Builder() {
            super(new Reference2IntOpenHashMap<>(0));
        }

        @Override
        public StatusEffectAmplifierMapSetting build() {
            return new StatusEffectAmplifierMapSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
