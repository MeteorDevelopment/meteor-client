/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.Collection;

public class ColorArgumentType implements ArgumentType<SettingColor> {
    private static final ColorArgumentType INSTANCE = new ColorArgumentType();

    private ColorArgumentType() {}

    public static ColorArgumentType color() {
        return INSTANCE;
    }

    public static <S> SettingColor get(CommandContext<S> context, String name) {
        return context.getArgument(name, SettingColor.class);
    }

    @Override
    public SettingColor parse(StringReader stringReader) throws CommandSyntaxException {
        int cursor = stringReader.getCursor();

        try {
            if (stringReader.readString().equals("rainbow")) {
                return new SettingColor().rainbow(true);
            }
        } catch (CommandSyntaxException ignored) {}
        stringReader.setCursor(cursor);

        int red = readInt(stringReader);
        int green = readInt(stringReader);
        int blue = readInt(stringReader);
        int alpha = 255;

        if (stringReader.canRead() && StringReader.isAllowedNumber(stringReader.peek())) {
            alpha = readInt(stringReader);
        }

        return new SettingColor(red, green, blue, alpha);
    }

    private static int readInt(StringReader reader) throws CommandSyntaxException {
        int i = reader.readInt();
        reader.skipWhitespace();
        if (i < 0) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, i, 0);
        } else if (i > 255) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, i, 255);
        } else {
            return i;
        }
    }

    @Override
    public Collection<String> getExamples() {
        return ColorSetting.SUGGESTIONS;
    }
}
