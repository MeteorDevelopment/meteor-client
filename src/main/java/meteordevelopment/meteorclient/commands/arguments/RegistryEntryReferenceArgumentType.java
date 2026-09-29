/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RegistryEntryReferenceArgumentType<T> implements ArgumentType<Holder.Reference<T>> {
    private static final RegistryEntryReferenceArgumentType<Enchantment> ENCHANTMENT = new RegistryEntryReferenceArgumentType<>(Registries.ENCHANTMENT);
    private static final RegistryEntryReferenceArgumentType<Attribute> ENTITY_ATTRIBUTE = new RegistryEntryReferenceArgumentType<>(Registries.ATTRIBUTE);
    private static final RegistryEntryReferenceArgumentType<Structure> STRUCTURE = new RegistryEntryReferenceArgumentType<>(Registries.STRUCTURE);
    private static final RegistryEntryReferenceArgumentType<EntityType<?>> ENTITY_TYPE = new RegistryEntryReferenceArgumentType<>(Registries.ENTITY_TYPE);
    private static final RegistryEntryReferenceArgumentType<MobEffect> MOB_EFFECT = new RegistryEntryReferenceArgumentType<>(Registries.MOB_EFFECT);

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType(
        (element, type) -> Component.translatableEscape("argument.resource.not_found", element, type)
    );
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
        (element, type, expectedType) -> Component.translatableEscape("argument.resource.invalid_type", element, type, expectedType)
    );
    private final ResourceKey<? extends Registry<T>> registryRef;

    private RegistryEntryReferenceArgumentType(ResourceKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static RegistryEntryReferenceArgumentType<Enchantment> enchantment() {
        return ENCHANTMENT;
    }

    public static RegistryEntryReferenceArgumentType<Attribute> entityAttribute() {
        return ENTITY_ATTRIBUTE;
    }

    public static RegistryEntryReferenceArgumentType<Structure> structure() {
        return STRUCTURE;
    }

    public static RegistryEntryReferenceArgumentType<EntityType<?>> entityType() {
        return ENTITY_TYPE;
    }

    public static RegistryEntryReferenceArgumentType<MobEffect> statusEffect() {
        return MOB_EFFECT;
    }

    public static Holder.Reference<Enchantment> getEnchantment(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ENCHANTMENT);
    }

    public static Holder.Reference<Attribute> getEntityAttribute(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ATTRIBUTE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.STRUCTURE);
    }

    public static Holder.Reference<EntityType<?>> getEntityType(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.ENTITY_TYPE);
    }

    public static Holder.Reference<MobEffect> getStatusEffect(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, Registries.MOB_EFFECT);
    }

    @SuppressWarnings("unchecked")
    private static <T> Holder.Reference<T> getRegistryEntry(CommandContext<?> context, String name, ResourceKey<Registry<T>> registryRef) throws CommandSyntaxException {
        Holder.Reference<T> reference = context.getArgument(name, Holder.Reference.class);
        ResourceKey<?> registryKey = reference.key();
        if (registryKey.isFor(registryRef)) {
            return reference;
        } else {
            throw INVALID_TYPE_EXCEPTION.create(registryKey.identifier(), registryKey.registry(), registryRef.identifier());
        }
    }

    @Override
    public Holder.Reference<T> parse(StringReader reader) throws CommandSyntaxException {
        Identifier identifier = Identifier.read(reader);
        ResourceKey<T> registryKey = ResourceKey.create(this.registryRef, identifier);
        return Minecraft.getInstance().getConnection().registryAccess()
            .lookupOrThrow(this.registryRef)
            .get(registryKey)
            .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(reader, identifier, this.registryRef.identifier()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(this.registryRef).listElementIds().map(ResourceKey::identifier), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
