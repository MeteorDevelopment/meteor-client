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
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RegistryEntryReferenceArgumentType<T> implements ArgumentType<RegistryEntry.Reference<T>> {
    private static final RegistryEntryReferenceArgumentType<Enchantment> ENCHANTMENT = new RegistryEntryReferenceArgumentType<>(RegistryKeys.ENCHANTMENT);
    private static final RegistryEntryReferenceArgumentType<EntityAttribute> ENTITY_ATTRIBUTE = new RegistryEntryReferenceArgumentType<>(RegistryKeys.ATTRIBUTE);
    private static final RegistryEntryReferenceArgumentType<Structure> STRUCTURE = new RegistryEntryReferenceArgumentType<>(RegistryKeys.STRUCTURE);
    private static final RegistryEntryReferenceArgumentType<EntityType<?>> ENTITY_TYPE = new RegistryEntryReferenceArgumentType<>(RegistryKeys.ENTITY_TYPE);
    private static final RegistryEntryReferenceArgumentType<StatusEffect> STATUS_EFFECT = new RegistryEntryReferenceArgumentType<>(RegistryKeys.STATUS_EFFECT);

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    public static final Dynamic2CommandExceptionType NOT_FOUND_EXCEPTION = new Dynamic2CommandExceptionType(
        (element, type) -> Text.stringifiedTranslatable("argument.resource.not_found", element, type)
    );
    public static final Dynamic3CommandExceptionType INVALID_TYPE_EXCEPTION = new Dynamic3CommandExceptionType(
        (element, type, expectedType) -> Text.stringifiedTranslatable("argument.resource.invalid_type", element, type, expectedType)
    );
    private final RegistryKey<? extends Registry<T>> registryRef;

    private RegistryEntryReferenceArgumentType(RegistryKey<? extends Registry<T>> registryRef) {
        this.registryRef = registryRef;
    }

    public static RegistryEntryReferenceArgumentType<Enchantment> enchantment() {
        return ENCHANTMENT;
    }

    public static RegistryEntryReferenceArgumentType<EntityAttribute> entityAttribute() {
        return ENTITY_ATTRIBUTE;
    }

    public static RegistryEntryReferenceArgumentType<Structure> structure() {
        return STRUCTURE;
    }

    public static RegistryEntryReferenceArgumentType<EntityType<?>> entityType() {
        return ENTITY_TYPE;
    }

    public static RegistryEntryReferenceArgumentType<StatusEffect> statusEffect() {
        return STATUS_EFFECT;
    }

    public static RegistryEntry.Reference<Enchantment> getEnchantment(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ENCHANTMENT);
    }

    public static RegistryEntry.Reference<EntityAttribute> getEntityAttribute(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ATTRIBUTE);
    }

    public static RegistryEntry.Reference<Structure> getStructure(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.STRUCTURE);
    }

    public static RegistryEntry.Reference<EntityType<?>> getEntityType(CommandContext<?> context, String name) throws CommandSyntaxException {
        return getRegistryEntry(context, name, RegistryKeys.ENTITY_TYPE);
    }

    public static RegistryEntry.Reference<StatusEffect> getStatusEffect(CommandContext<?> context, String name) throws CommandSyntaxException {
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
    public RegistryEntry.Reference<T> parse(StringReader reader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(reader);
        RegistryKey<T> registryKey = RegistryKey.of(this.registryRef, identifier);
        return MinecraftClient.getInstance().getNetworkHandler().getRegistryManager()
            .getWrapperOrThrow(this.registryRef)
            .getOptional(registryKey)
            .orElseThrow(() -> NOT_FOUND_EXCEPTION.createWithContext(reader, identifier, this.registryRef.getValue()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(MinecraftClient.getInstance().getNetworkHandler().getRegistryManager().getWrapperOrThrow(this.registryRef).streamKeys().map(RegistryKey::getValue), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
