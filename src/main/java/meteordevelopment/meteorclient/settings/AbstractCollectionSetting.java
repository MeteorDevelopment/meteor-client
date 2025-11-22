/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;


import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class AbstractCollectionSetting<T> extends Setting<T> {
    public AbstractCollectionSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    protected String[] components(String value) {
        return value.split(",");
    }

    @Override
    public CompletableFuture<Suggestions> buildSuggestions(SuggestionsBuilder builder) {
        String given = builder.getInput().substring(builder.getStart());

        String[] components = components(given);

        if (components.length == 0) return builder.buildFuture();

        String last = components[components.length - 1];

        last = last.trim();

        Iterable<Identifier> identifiers = getIdentifierSuggestions();

        builder = builder.createOffset(builder.getStart() + given.length() - last.length());
        SuggestionsBuilder builderDuplicatedDueToArbitraryLambdaCaptureRestrictions = builder;

        if (identifiers != null) {
            CommandSource.forEachMatching(identifiers, last, id -> id, id -> builderDuplicatedDueToArbitraryLambdaCaptureRestrictions.suggest(id.toString()));
            return builderDuplicatedDueToArbitraryLambdaCaptureRestrictions.buildFuture();
        }

        Iterable<String> suggestions = getSuggestions();

        if (suggestions != null) {
            for(String s : suggestions) {
                if (CommandSource.shouldSuggest(last, s.toLowerCase(Locale.ROOT))) {
                    builderDuplicatedDueToArbitraryLambdaCaptureRestrictions.suggest(s);
                }
            }
        }

        return builderDuplicatedDueToArbitraryLambdaCaptureRestrictions.buildFuture();
    }
}
