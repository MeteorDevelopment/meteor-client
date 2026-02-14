/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RichTextContent implements TextContent {
    private static final boolean DEBUG_MISSING_ENTRIES = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("meteor.lang.debug");
    protected final Object[] args;

    private List<StringVisitable> parts = ImmutableList.of();

    protected RichTextContent(Object... args) {
        this.args = args;
    }

    protected abstract boolean shouldUpdate();

    protected abstract String getTemplate();

    protected void update(String template) {
        try {
            ImmutableList.Builder<StringVisitable> builder = ImmutableList.builder();
            this.forEachPart(template, builder);
            this.parts = builder.build();
        } catch (IllegalArgumentException e) {
            if (DEBUG_MISSING_ENTRIES) {
                MeteorClient.LOG.warn("Error formatting text", e);
            }
            this.parts = ImmutableList.of(StringVisitable.plain(template));
        }
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
        if (this.shouldUpdate()) {
            this.update(this.getTemplate());
        }

        for (StringVisitable stringVisitable : this.parts) {
            Optional<T> result = stringVisitable.visit(visitor, style);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
        if (this.shouldUpdate()) {
            this.update(this.getTemplate());
        }

        for (StringVisitable stringVisitable : this.parts) {
            Optional<T> result = stringVisitable.visit(visitor);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public MapCodec<? extends TextContent> getCodec() {
        return null;
    }

    private static final StringVisitable LITERAL_PERCENT_SIGN = StringVisitable.plain("%");
    private static final StringVisitable NULL_ARGUMENT = StringVisitable.plain("null");
    // %, optional position argument (\d$), string format (s) || percent literal (%|$)
    private static final Pattern ARG_FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([s%]|$)");

    private void forEachPart(String template, ImmutableList.Builder<StringVisitable> builder) {
        Matcher matcher = ARG_FORMAT.matcher(template);

        try {
            int argPosition = 0;
            int charIndex = 0;

            while (matcher.find(charIndex)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > charIndex) {
                    String string = template.substring(charIndex, start);
                    if (string.indexOf(37) != -1) {
                        throw new IllegalArgumentException(string);
                    }

                    builder.add(StringVisitable.plain(string));
                }

                String string = matcher.group(2);
                String format = template.substring(start, end);
                if ("%".equals(string) && "%%".equals(format)) {
                    builder.add(LITERAL_PERCENT_SIGN);
                } else {
                    String positionArgument = matcher.group(1);
                    int index = positionArgument != null ? Integer.parseInt(positionArgument) - 1 : argPosition++;
                    if (index < 0 || index >= this.args.length) {
                        throw exception(template, index);
                    }

                    Object argument = this.args[index];

                    if (string.equals("s")) {
                        StringVisitable visitableArgument = argument instanceof StringVisitable visitable ? visitable
                            : argument == null ? NULL_ARGUMENT : StringVisitable.plain(argument.toString());

                        builder.add(visitableArgument);
                    } else {
                        throw exception(template, "Unsupported format: '" + format + "'");
                    }
                }

                charIndex = end;
            }

            if (charIndex < template.length()) {
                String rest = template.substring(charIndex);
                if (rest.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                builder.add(StringVisitable.plain(rest));
            }
        } catch (IllegalArgumentException e) {
            throw exception(template, e);
        }
    }

    private static IllegalArgumentException exception(String template, String cause) {
        return new IllegalArgumentException(String.format(Locale.ROOT, "Error parsing: %s: %s", template, cause));
    }

    private static IllegalArgumentException exception(String template, int index) {
        return new IllegalArgumentException(String.format(Locale.ROOT, "Invalid index %d requested for %s", index, template));
    }

    private static IllegalArgumentException exception(String template, Throwable cause) {
        return new IllegalArgumentException(String.format(Locale.ROOT, "Error while parsing: %s", template), cause);
    }
}
