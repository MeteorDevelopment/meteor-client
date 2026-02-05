/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorTranslatableTextContent implements TextContent {
    private static final boolean DEBUG_MISSING_ENTRIES = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("meteor.lang.debug");
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    private final boolean styledArgs;

    private String cachedLanguage;
    private List<StringVisitable> translations = ImmutableList.of();

    public MeteorTranslatableTextContent(String key, @Nullable String fallback, Object... args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;

        boolean hasStyledArgs = false;
        for (Object o : args) {
            if (o instanceof Text) {
                hasStyledArgs = true;
                break;
            }
        }
        this.styledArgs = hasStyledArgs;
    }

    public MeteorTranslatableTextContent(String key, Object... args) {
        this(key, null, args);
    }

    public MeteorTranslatableTextContent(String key, @Nullable String fallback, Object[] args, boolean styledArgs) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
        this.styledArgs = styledArgs;
    }

    private void updateTranslations() {
        if (!mc.options.language.equals(this.cachedLanguage)) {
            cachedLanguage = mc.options.language;
            if (styledArgs) {
                String template = fallback == null ? MeteorTranslations.translate(key) : MeteorTranslations.translate(key, fallback);

                try {
                    ImmutableList.Builder<StringVisitable> builder = ImmutableList.builder();
                    this.forEachPart(template, builder::add);
                    this.translations = builder.build();
                } catch (TranslationException e) {
                    if (DEBUG_MISSING_ENTRIES) {
                        MeteorClient.LOG.warn("Error translating text", e);
                    }
                    this.translations = ImmutableList.of(StringVisitable.plain(template));
                }
            } else {
                String template = fallback == null ? MeteorTranslations.translate(key, args) : MeteorTranslations.translate(key, fallback, args);

                this.translations = ImmutableList.of(ChatUtils.formatMsg(template, Style.EMPTY));
            }
        }
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
        updateTranslations();

        for (StringVisitable stringVisitable : translations) {
            Optional<T> result = stringVisitable.visit(visitor, style);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
        updateTranslations();

        for (StringVisitable stringVisitable : translations) {
            Optional<T> result = stringVisitable.visit(visitor);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public MapCodec<? extends TextContent> getCodec() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MeteorTranslatableTextContent component)) return false;
        return Objects.equals(this.key, component.key) && Objects.equals(this.fallback, component.fallback) && Arrays.equals(this.args, component.args);
    }

    @Override
    public String toString() {
        return "MeteorTranslatableTextComponent[key=" + key + ", fallback=" + fallback + ", args=" + Arrays.toString(args) + "]";
    }


    private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
    private static final StringVisitable NULL_ARGUMENT = StringVisitable.plain("null");
    // %, optional position argument (\d$), string format (s) || percent literal (%|$)
    private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([s%]|$)");

    private void forEachPart(String translation, Consumer<StringVisitable> partsConsumer) {
        Matcher matcher = ARG_FORMAT.matcher(translation);

        try {
            int argPosition = 0;
            int charIndex = 0;

            while (matcher.find(charIndex)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > charIndex) {
                    String string = translation.substring(charIndex, start);
                    if (string.indexOf(37) != -1) {
                        throw new IllegalArgumentException(string);
                    }

                    partsConsumer.accept(StringVisitable.plain(string));
                }

                String string = matcher.group(2);
                String string2 = translation.substring(start, end);
                if ("%".equals(string) && "%%".equals(string2)) {
                    partsConsumer.accept(LITERAL_PERCENT_SIGN);
                } else {
                    String positionArgument = matcher.group(1);
                    int index = positionArgument != null ? Integer.parseInt(positionArgument) - 1 : argPosition++;
                    if (index < 0 || index >= this.args.length) {
                        throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), index);
                    }

                    Object argument = this.args[index];

                    if (string.equals("s")) { // fast path
                        StringVisitable visitableArgument = argument instanceof StringVisitable visitable ? visitable
                            : argument == null ? NULL_ARGUMENT : StringVisitable.plain(argument.toString());

                        partsConsumer.accept(visitableArgument);
                    } else {
                        throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), "Unsupported format: '" + string2 + "'");
                    }
                }

                charIndex = end;
            }

            if (charIndex < translation.length()) {
                String string4 = translation.substring(charIndex);
                if (string4.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                partsConsumer.accept(StringVisitable.plain(string4));
            }
        } catch (IllegalArgumentException e) {
            throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), e);
        }
    }
}
