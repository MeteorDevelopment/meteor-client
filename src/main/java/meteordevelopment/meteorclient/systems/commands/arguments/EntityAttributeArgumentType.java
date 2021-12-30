/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class EntityAttributeArgumentType implements ArgumentType<EntityAttribute> {
    private static final Collection<String> EXAMPLES = Arrays.asList("max_health", "movement_speed");
    private static final DynamicCommandExceptionType NO_SUCH_ATTRIBUTE = new DynamicCommandExceptionType(o ->
        new LiteralText("Attribute with name '" + o + "' doesn't exist.")
    );

    public static EntityAttributeArgumentType attribute() {
        return new EntityAttributeArgumentType();
    }

    public static EntityAttribute getAttribute(CommandContext<?> context, String name) {
        return context.getArgument(name, EntityAttribute.class);
    }

    public EntityAttribute parse(StringReader stringReader) throws CommandSyntaxException {
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        String argument = stringReader.readString();

        return Registry.ATTRIBUTE.getOrEmpty(identifier).orElseThrow(() -> NO_SUCH_ATTRIBUTE.create(argument));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(Registry.ATTRIBUTE.getIds(), builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
