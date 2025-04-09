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
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.settings.FontFaceSetting;
import net.minecraft.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FontFaceArgumentType implements ArgumentType<FontFace> {
    private static final FontFaceArgumentType INSTANCE = new FontFaceArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_FONT_FACE_EXCEPTION = new DynamicCommandExceptionType(o -> new LiteralMessage("No such font face: ' " + o + "'"));

    private FontFaceArgumentType() {}

    public static FontFaceArgumentType fontFace() {
        return INSTANCE;
    }

    public static <S> FontFace get(CommandContext<S> context, String name) {
        return context.getArgument(name, FontFace.class);
    }

    @Override
    public FontFace parse(StringReader stringReader) throws CommandSyntaxException {
        String value = stringReader.readString();

        String[] split = value.split("-");
        if (split.length != 2) throw NO_SUCH_FONT_FACE_EXCEPTION.createWithContext(stringReader, value);

        for (FontFamily family : Fonts.FONT_FAMILIES) {
            if (family.getName().replace(" ", "").equals(split[0])) {
                try {
                    return family.get(FontInfo.Type.valueOf(split[1]));
                }
                catch (IllegalArgumentException ignored) {
                    throw NO_SUCH_FONT_FACE_EXCEPTION.createWithContext(stringReader, value);
                }
            }
        }

        throw NO_SUCH_FONT_FACE_EXCEPTION.createWithContext(stringReader, value);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        BiConsumer<FontFamily, Consumer<FontFace>> mapper = (family, c) -> {
            for (FontInfo.Type type : FontInfo.Type.values()) {
                @Nullable FontFace font = family.get(type);
                if (font != null) {
                    c.accept(font);
                }
            }
        };

        return CommandSource.suggestMatching(Fonts.FONT_FAMILIES.stream().mapMulti(mapper).map(FontFaceArgumentType::stringify), builder);
    }

    private static String stringify(FontFace fontFace) {
        String family = fontFace.info.family().replace(" ", "");
        String type = fontFace.info.type().toString().replace(" ", "");
        return family + "-" + type;
    }

    @Override
    public Collection<String> getExamples() {
        return FontFaceSetting.EXAMPLES;
    }
}
