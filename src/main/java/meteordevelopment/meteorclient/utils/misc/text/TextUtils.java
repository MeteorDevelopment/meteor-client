/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.*;

/**
 * Some utilities for {@link Text}
 */
public class TextUtils {
    private TextUtils() {
    }

    public static List<ColoredText> toColoredTextList(Text text) {
        Deque<ColoredText> stack = new ArrayDeque<>();
        List<ColoredText> coloredTexts = new ArrayList<>();
        preOrderTraverse(text, stack, coloredTexts);
        coloredTexts.removeIf(e -> e.text().isEmpty());
        return coloredTexts;
    }

    /**
     * Parses a given {@link OrderedText} into its {@link Text} equivalent.
     *
     * @param orderedText the {@link OrderedText} to parse.
     * @return The {@link Text} equivalent of the {@link OrderedText} parameter.
     */
    public static MutableText parseOrderedText(OrderedText orderedText) {
        MutableText parsedText = Text.empty();
        orderedText.accept((i, style, codePoint) -> {
            parsedText.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });
        return parsedText;
    }

    /**
     * Returns the {@link Color} that is most prevalent through the given {@link Text}
     *
     * @param text the {@link Text} to scan through
     * @return You know what it returns. Read the docs! Also, returns white if the internal {@link Object2IntMap.Entry} is null
     */
    public static Color getMostPopularColor(Text text) {
        Object2IntMap.Entry<Color> biggestEntry = null;
        for (var entry : getColoredCharacterCount(toColoredTextList(text)).object2IntEntrySet()) {
            if (biggestEntry == null) biggestEntry = entry;
            else if (entry.getIntValue() > biggestEntry.getIntValue()) biggestEntry = entry;
        }
        return biggestEntry == null ? new Color(255, 255, 255) : biggestEntry.getKey();
    }

    /**
     * Takes a {@link List} of {@link ColoredText} and returns a {@link HashMap}, where the keys are all the existing {@link Color}s in the
     * aforementioned list, and the corresponding keys are the number of characters that have that color.
     *
     * @param coloredTexts The list of {@link ColoredText} to obtain the color count of. Best paired with the output from {@link #toColoredTextList(Text)}
     * @return a {@link Map} whose keys are colors (and the set of keys being all possible colors used in the list, thus all colors in the text,
     * if the argument for this function is fed from the return from {@link #toColoredTextList(Text)}), and the corresponding values being {@link Integer}s
     * representing the number of occurrences of text that bear that color. The order of the keys are in no particular order
     */
    public static Object2IntMap<Color> getColoredCharacterCount(List<ColoredText> coloredTexts) {
        Object2IntMap<Color> colorCount = new Object2IntOpenHashMap<>();

        for (ColoredText coloredText : coloredTexts) {
            if (colorCount.containsKey(coloredText.color())) {
                // Since color was already catalogued, simply update the record by adding the length of the new text segment to the old one
                colorCount.put(coloredText.color(), colorCount.getInt(coloredText.color()) + coloredText.text().length());
            } else {
                // Add new entry to the hashmap
                colorCount.put(coloredText.color(), coloredText.text().length());
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
    private static void preOrderTraverse(Text text, Deque<ColoredText> stack, List<ColoredText> coloredTexts) {
        if (text == null)
            return;

        // Do actions here
        String textString = text.getString();

        TextColor mcTextColor = text.getStyle().getColor();


        // If mcTextColor is null, the color should be inherited from its parent. In this case, the path of the recursion is stored on the stack,
        // with the current element's parent at the top, so simply peek it if possible. If not, there is no parent element,
        // and with no color, use the default of white.
        Color textColor;
        if (mcTextColor == null) {
            if (stack.isEmpty())
                // No color defined, use default white
                textColor = new Color(255, 255, 255);
            else
                // Use parent color
                textColor = stack.peek().color();
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
