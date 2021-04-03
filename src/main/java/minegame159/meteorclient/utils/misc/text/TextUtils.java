/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc.text;

import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.*;

/**
 * Some utilities for {@link Text}
 */
public class TextUtils {
    public static List<ColoredText> toColoredTextList(Text text) {
        Stack<ColoredText> stack = new Stack<>();
        List<ColoredText> coloredTexts = new ArrayList<>();
        preOrderTraverse(text, stack, coloredTexts);
        coloredTexts.removeIf(e -> e.getText().equals(""));
        return coloredTexts;
    }

    /**
     * Returns the {@link Color} that is most prevalent through the given {@link Text}
     *
     * @param text the {@link Text} to scan through
     * @return You know what it returns. Read the docs! Also, returns white if the internal {@link Optional} is empty
     */
    public static Color getMostPopularColor(Text text) {
        Comparator<Integer> integerComparator = Comparator.naturalOrder();
        Optional<Map.Entry<Color, Integer>> optionalColor = getColoredCharacterCount(toColoredTextList(text))
                .entrySet().stream()
                .max((a, b) -> integerComparator.compare(a.getValue(), b.getValue()));

        return optionalColor.map(Map.Entry::getKey).orElse(new Color(255, 255, 255));
    }

    /**
     * Takes a {@link List}<{@link ColoredText}> and returns a {@link HashMap}, where the keys are all the existing {@link Color}s in the
     * aforementioned list, and the corresponding keys are the number of characters that have that color.
     *
     * @param coloredTexts The list of {@link ColoredText} to obtain the color count of. Best paired with the output from {@link #toColoredTextList(Text)}
     * @return a {@link Map} whose keys are colors (and the set of keys being all possible colors used in the list, thus all colors in the text,
     * if the argument for this function is fed from the return from {@link #toColoredTextList(Text)}), and the corresponding values being {@link Integer}s
     * representing the number of occurrences of text that bear that color. The order of the keys are in no particular order
     */
    public static Map<Color, Integer> getColoredCharacterCount(List<ColoredText> coloredTexts) {
        Map<Color, Integer> colorCount = new HashMap<>();

        for (ColoredText coloredText : coloredTexts) {
            if (colorCount.containsKey(coloredText.getColor())) {
                // Since color was already catalogued, simply update the record by adding the length of the new text segment to the old one
                colorCount.put(coloredText.getColor(), colorCount.get(coloredText.getColor()) + coloredText.getText().length());
            } else {
                // Add new entry to the hashmap
                colorCount.put(coloredText.getColor(), coloredText.getText().length());
            }
        }

        return colorCount;
    }

    /**
     * Performs a pre-order text traversal of {@link Text} components and ref-returns a sequential list
     * of {@link ColoredText}, such that one could know the text and its color by iterating over the list.
     *
     * @param text         The text to flatten
     * @param stack        An empty stack. This is used by the recursive algorithm to keep track of the parents of the current iteration
     * @param coloredTexts The list of colored text to return
     */
    private static void preOrderTraverse(Text text, Stack<ColoredText> stack, List<ColoredText> coloredTexts) {
        if (text == null)
            return;

        // Do actions here
        String textString = text.asString();

        TextColor mcTextColor = text.getStyle().getColor();


        // If mcTextColor is null, the color should be inherited from its parent. In this case, the path of the recursion is stored on the stack,
        // with the current element's parent at the top, so simply peek it if possible. If not, there is no parent element,
        // and with no color, use the default of white.
        Color textColor;
        if (mcTextColor == null) {
            if (stack.empty())
                // No color defined, use default white
                textColor = new Color(255, 255, 255);
            else
                // Use parent color
                textColor = stack.peek().getColor();
        } else {
            // Has a color defined, so use that
            textColor = new Color((text.getStyle().getColor().getRgb()) | 0xFF000000); // Sets alpha to max. Some damn reason Color's packed ctor is in ARGB format, not RGBA
        }

        ColoredText coloredText = new ColoredText(textString, textColor);
        coloredTexts.add(coloredText);
        stack.push(coloredText); // For the recursion algorithm's child, the current coloredText is its parent, so add to stack
        // Recursively traverse
        for (Text child : text.getSiblings())
            preOrderTraverse(child, stack, coloredTexts);

        stack.pop();
    }
}