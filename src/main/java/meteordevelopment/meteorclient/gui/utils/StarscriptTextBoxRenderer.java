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
import meteordevelopment.starscript.compiler.Parser;

import java.util.ArrayList;
import java.util.List;

public class StarscriptTextBoxRenderer implements WTextBox.Renderer {
    private static final String[] KEYWORDS = { "null", "true", "false", "and", "or" };
    private static final Color RED = new Color(225, 25, 25);

    private String lastText;
    private final List<Section> sections = new ArrayList<>();

    @Override
    public void render(GuiRenderer renderer, double x, double y, String text, Color color) {
        if (lastText == null || !lastText.equals(text)) generate(renderer.theme, text, color);

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

    private void generate(GuiTheme theme, String text, Color defaultColor) {
        lastText = text;
        sections.clear();

        Parser.Result result = Parser.parse(text);

        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean addChar = true;
            int charDepth = depth;

            if (result.hasErrors()) {
                if (i == result.errors.get(0).character) {
                    sections.add(new Section(sb.toString(), charDepth > 0 ? theme.starscriptTextColor() : defaultColor));
                    sb.setLength(0);
                }
                else if (i > result.errors.get(0).character) {
                    sb.append(c);
                    continue;
                }
            }

            Section section = null;

            switch (c) {
                case '#' -> {
                    while (i + 1 < text.length()) {
                        char ch = text.charAt(i + 1);
                        if (isDigit(ch)) {
                            sb2.append(ch);
                            i++;
                        }
                        else break;
                    }

                    if (!sb2.isEmpty()) {
                        String str = sb2.toString();
                        section = new Section("#" + str, TextHud.getSectionColor(Integer.parseInt(str)));
                        sb2.setLength(0);
                    }
                }
                case '{', '}'  -> {
                    if (c == '{') depth++;
                    else depth--;

                    section = new Section(Character.toString(c), theme.starscriptBraceColor());
                }
            }

            if (section == null && depth > 0) {
                if (c == '.') {
                    sections.add(new Section(sb.toString(), theme.starscriptAccessedObjectColor()));
                    sections.add(new Section(".", theme.starscriptDotColor()));

                    sb.setLength(0);
                    addChar = false;
                }
                else {
                    switch (c) {
                        case '(', ')' -> section = new Section(Character.toString(c), theme.starscriptParenthesisColor());
                        case ',' -> section = new Section(",", theme.starscriptCommaColor());
                        case '+', '-', '*', '/', '%', '^', '?', ':' -> {
                            if (c == '-' && i + 1 < text.length() && isDigit(text.charAt(i + 1))) break;
                            section = new Section(Character.toString(c), theme.starscriptOperatorColor());
                        }
                        case '=', '!', '>', '<' -> {
                            boolean equals = i + 1 < text.length() && text.charAt(i + 1) == '=';
                            if (equals) i++;

                            section = new Section(equals ? (c + "=") : Character.toString(c), theme.starscriptOperatorColor());
                        }
                        case '"', '\'' -> {
                            sb2.append(c);
                            while (i + 1 < text.length()) {
                                char ch = text.charAt(i + 1);
                                if (ch != '"' && ch != '\'') {
                                    sb2.append(ch);
                                    i++;
                                } else {
                                    sb2.append(ch);
                                    i++;
                                    break;
                                }
                            }

                            section = new Section(sb2.toString(), theme.starscriptStringColor());
                            sb2.setLength(0);
                        }
                    }

                    if (section == null) {
                        if (isDigit(c) || (c == '-' && i + 1 < text.length() && isDigit(text.charAt(i + 1)))) {
                            sb2.append(c);

                            while (i + 1 < text.length()) {
                                char ch = text.charAt(i + 1);
                                if (isDigit(ch)) {
                                    sb2.append(ch);
                                    i++;
                                } else break;
                            }

                            if (i + 1 < text.length() && text.charAt(i + 1) == '.') {
                                if (i + 2 < text.length() && isDigit(text.charAt(i + 2))) {
                                    sb2.append('.');
                                    i++;

                                    while (i + 1 < text.length()) {
                                        char ch = text.charAt(i + 1);
                                        if (isDigit(ch)) {
                                            sb2.append(ch);
                                            i++;
                                        } else break;
                                    }
                                }
                            }

                            section = new Section(sb2.toString(), theme.starscriptNumberColor());
                            sb2.setLength(0);
                        } else {
                            for (String keyword : KEYWORDS) {
                                if (isKeyword(text, i, keyword)) {
                                    section = new Section(keyword, theme.starscriptKeywordColor());
                                    i += keyword.length() - 1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (section != null) {
                if (!sb.isEmpty()) {
                    sections.add(new Section(sb.toString(), charDepth > 0 ? theme.starscriptTextColor() : defaultColor));
                    sb.setLength(0);
                }

                sections.add(section);
            }
            else if (addChar) sb.append(c);
        }

        if (!sb.isEmpty()) sections.add(new Section(sb.toString(), result.hasErrors() ? RED : defaultColor));
    }

    private boolean isKeyword(String text, int i, String keyword) {
        if (i > 0) {
            char c = text.charAt(i - 1);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') return false;
        }

        for (int j = 0; j < keyword.length(); j++) {
            if (i + j >= text.length() || text.charAt(i + j) != keyword.charAt(j)) return false;
        }

        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private record Section(String text, Color color) {}
}
