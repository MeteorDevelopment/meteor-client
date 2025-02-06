/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class RegistryEntryArgumentType<T> implements ArgumentType<RegistryEntry.Reference<T>> {
    private static final RegistryEntryArgumentType<Block> BLOCK = new RegistryEntryArgumentType<>(Registries.BLOCK);
    private static final RegistryEntryArgumentType<BlockEntityType<?>> BLOCK_ENTITY_TYPE = new RegistryEntryArgumentType<>(Registries.BLOCK_ENTITY_TYPE);
    private static final RegistryEntryArgumentType<EntityType<?>> ENTITY_TYPE = new RegistryEntryArgumentType<>(Registries.ENTITY_TYPE);
    private static final RegistryEntryArgumentType<Item> ITEM = new RegistryEntryArgumentType<>(Registries.ITEM);
    private static final RegistryEntryArgumentType<ParticleType<?>> PARTICLE_TYPE = new RegistryEntryArgumentType<>(Registries.PARTICLE_TYPE);
    private static final RegistryEntryArgumentType<SoundEvent> SOUND_EVENT = new RegistryEntryArgumentType<>(Registries.SOUND_EVENT);
    private static final RegistryEntryArgumentType<StatusEffect> STATUS_EFFECT = new RegistryEntryArgumentType<>(Registries.STATUS_EFFECT);

    public static final DynamicCommandExceptionType NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
        element -> new LiteralMessage("Not found exception type shii " + element)
    );
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
        (element, type, expectedType) -> Text.stringifiedTranslatable("argument.resource.invalid_type", element, type, expectedType)
    );
    private final Registry<T> registry;

    private RegistryEntryArgumentType(Registry<T> registry) {
        this.registry = registry;
    }

    public static RegistryEntryArgumentType<Block> block() {
        return BLOCK;
    }

    public static RegistryEntryArgumentType<BlockEntityType<?>> blockEntityType() {
        return BLOCK_ENTITY_TYPE;
    }

    public static RegistryEntryArgumentType<EntityType<?>> entityType() {
        return ENTITY_TYPE;
    }

    public static RegistryEntryArgumentType<Item> item() {
        return ITEM;
    }

    public static RegistryEntryArgumentType<ParticleType<?>> particleType() {
        return PARTICLE_TYPE;
    }

    public static RegistryEntryArgumentType<SoundEvent> soundEvent() {
        return SOUND_EVENT;
    }

    public static RegistryEntryArgumentType<StatusEffect> statusEffect() {
        return STATUS_EFFECT;
    }

    public static <S> RegistryEntry.Reference<Block> getBlock(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.BLOCK);
    }

    public static <S> RegistryEntry.Reference<BlockEntityType<?>> getBlockEntityType(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.BLOCK_ENTITY_TYPE);
    }

    public static <S> RegistryEntry.Reference<EntityType<?>> getEntityType(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
    }

    public static <S> RegistryEntry.Reference<Item> getItem(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ITEM);
    }

    public static <S> RegistryEntry.Reference<ParticleType<?>> getParticleType(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.PARTICLE_TYPE);
    }

    public static <S> RegistryEntry.Reference<SoundEvent> getSoundEvent(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.SOUND_EVENT);
    }

    public static <S> RegistryEntry.Reference<StatusEffect> getStatusEffect(CommandContext<S> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.STATUS_EFFECT);
    }

    private static <T> RegistryEntry.Reference<T> getRegistryEntry(CommandContext<?> context, String name, RegistryKey<Registry<T>> registryRef) throws CommandSyntaxException {
        RegistryEntry.Reference<T> reference = context.getArgument(name, RegistryEntry.Reference.class);
        RegistryKey<?> registryKey = reference.registryKey();
        if (registryKey.isOf(registryRef)) {
            return reference;
        } else {
            throw INVALID_TYPE_EXCEPTION.create(registryKey.getValue(), registryKey.getRegistry(), registryRef.getValue());
        }
    }

    @Override
    public RegistryEntry.Reference<T> parse(StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        return this.registry.getEntry(identifier)
            .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(stringReader, identifier));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(this.registry.streamKeys().map(RegistryKey::getValue).map(Object::toString), builder);
    }
}
