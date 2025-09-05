/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.meteordev.starscript.utils.SemanticToken;
import org.meteordev.starscript.utils.SemanticTokenProvider;
import org.meteordev.starscript.utils.SemanticTokenType;

import java.util.ArrayList;
import java.util.List;

public class StarscriptTextBoxRenderer implements WTextBox.Renderer {
    private static final Color RED = new Color(225, 25, 25);

    private final List<SemanticToken> tokens = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

    private String lastText;

    @Override
    public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
        // Generate
        if (lastText == null || !lastText.equals(text)) {
            lastText = text;

            SemanticTokenProvider.get(text, tokens);
            convertTokensToSections(renderer.theme);
        }

        // Render
        for (Section section : sections) {
            renderer.text(section.text, x, y, section.color, false);
            x += renderer.theme.textWidth(section.text);
        }
    }

    @Override
    public List<String> getCompletions(String text, int position) {
        List<String> completions = new ArrayList<>();

        MeteorStarscript.ss.getCompletions(text, position, (completion, function) -> {
            completions.add(function ? completion + "(" : completion);
        });

        completions.sort(String::compareToIgnoreCase);

        return completions;
    }

    private void convertTokensToSections(GuiTheme theme) {
        sections.clear();

        int start = 0;

        for (SemanticToken token : tokens) {
            if (start != token.start) {
                sections.add(new Section(
                    lastText.substring(start, token.start),
                    theme.starscriptTextColor()
                ));
            }

            String text = lastText.substring(token.start, token.end);

            sections.add(new Section(
                text,
                getColorForToken(theme, token.type, text)
            ));

            start = token.end;
        }

        if (start < lastText.length()) {
            sections.add(new Section(
                lastText.substring(start),
                theme.starscriptTextColor()
            ));
        }
    }

    private static Color getColorForToken(GuiTheme theme, SemanticTokenType type, String text) {
        return switch (type) {
            case Dot -> theme.starscriptDotColor();
            case Comma -> theme.starscriptCommaColor();
            case Operator -> theme.starscriptOperatorColor();
            case String -> theme.starscriptStringColor();
            case Number -> theme.starscriptNumberColor();
            case Keyword -> theme.starscriptKeywordColor();
            case Paren -> theme.starscriptParenthesisColor();
            case Brace -> theme.starscriptBraceColor();
            case Identifier -> theme.starscriptTextColor();
            case Map -> theme.starscriptAccessedObjectColor();
            case Section -> {
                if (text.startsWith("#")) {
                    text = text.substring(1);
                }

                try {
                    yield TextHud.getSectionColor(Integer.parseInt(text));
                } catch (NumberFormatException ignored) {}

                yield theme.starscriptTextColor();
            }
            case Error -> RED;
        };
    }

    private record Section(String text, Color color) {}
}
