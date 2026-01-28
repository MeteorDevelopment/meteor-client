/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeteorTranslatableTextComponent implements TextContent {
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    private final boolean styledArgs;

    private String cachedLanguage;
    private String translation;
    private List<StringVisitable> translations = ImmutableList.of();

    public MeteorTranslatableTextComponent(String key, @Nullable String fallback, Object... args) {
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

    public MeteorTranslatableTextComponent(String key, Object... args) {
        this(key, null, args);
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
                    this.translations = ImmutableList.of(StringVisitable.plain(template));
                }
            } else {
                translation = fallback == null ? MeteorTranslations.translate(key, args) : MeteorTranslations.translate(key, fallback, args);
            }
        }
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
        updateTranslations();

        if (styledArgs) {
            for (StringVisitable stringVisitable : translations) {
                Optional<T> result = stringVisitable.visit(visitor, style);
                if (result.isPresent()) return result;
            }
            return Optional.empty();
        } else {
            return visitor.accept(style, translation);
        }
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
        updateTranslations();

        if (styledArgs) {
            for (StringVisitable stringVisitable : translations) {
                Optional<T> result = stringVisitable.visit(visitor);
                if (result.isPresent()) return result;
            }
            return Optional.empty();
        } else {
            return visitor.accept(translation);
        }
    }

    @Override
    public MapCodec<? extends TextContent> getCodec() {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MeteorTranslatableTextComponent component)) return false;
        return Objects.equals(this.key, component.key) && Objects.equals(this.fallback, component.fallback) && Arrays.equals(this.args, component.args);
    }

    @Override
    public String toString() {
        return "MeteorTranslatableTextComponent[key=" + key + ", fallback=" + fallback + ", args=" + Arrays.toString(args) + "]";
    }

    /// Bunch of bullshit from {@link net.minecraft.text.TranslatableTextContent}
    private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
    private static final StringVisitable NULL_ARGUMENT = StringVisitable.plain("null");
    private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private void forEachPart(String translation, Consumer<StringVisitable> partsConsumer) {
        Matcher matcher = ARG_FORMAT.matcher(translation);

        try {
            int i = 0;
            int j = 0;

            while (matcher.find(j)) {
                int k = matcher.start();
                int l = matcher.end();
                if (k > j) {
                    String string = translation.substring(j, k);
                    if (string.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    partsConsumer.accept(StringVisitable.plain(string));
                }

                String string = matcher.group(2);
                String string2 = translation.substring(k, l);
                if ("%".equals(string) && "%%".equals(string2)) {
                    partsConsumer.accept(LITERAL_PERCENT_SIGN);
                } else {
                    if (!"s".equals(string)) {
                        throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), "Unsupported format: '" + string2 + "'");
                    }

                    String string3 = matcher.group(1);
                    int m = string3 != null ? Integer.parseInt(string3) - 1 : i++;
                    partsConsumer.accept(this.getArg(m));
                }

                j = l;
            }

            if (j < translation.length()) {
                String string4 = translation.substring(j);
                if (string4.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                partsConsumer.accept(StringVisitable.plain(string4));
            }
        } catch (IllegalArgumentException e) {
            throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), e);
        }
    }

    public final StringVisitable getArg(int index) {
        if (index >= 0 && index < this.args.length) {
            Object object = this.args[index];
            if (object instanceof Text text) {
                return text;
            } else {
                return object == null ? NULL_ARGUMENT : StringVisitable.plain(object.toString());
            }
        } else {
            throw new TranslationException(new TranslatableTextContent(this.key, this.fallback, this.args), index);
        }
    }
}
