/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

/**
 * An extension of {@link net.minecraft.text.StringVisitable.StyledVisitor} with access to the underlying {@link Text} objects.
 * @param <T> the optional short circuit return type, to match the semantics of {@link net.minecraft.text.StringVisitable.Visitor} and {@link net.minecraft.text.StringVisitable.StyledVisitor}.
 * @author Crosby
 */
@FunctionalInterface
public interface TextVisitor<T> {
    Optional<T> accept(Text text, Style style, String string);

    static <T> Optional<T> visit(Text text, TextVisitor<T> visitor, Style baseStyle) {
        Queue<Text> queue = collectSiblings(text);
        return text.visit((style, string) -> visitor.accept(queue.remove(), style, string), baseStyle);
    }

    /**
     * Collapses the tree of {@link Text} siblings into a one dimensional LIFO {@link Queue}
     * @param text the text
     * @return the text and its siblings in the order they appear when rendered.
     */
    static Queue<Text> collectSiblings(Text text) {
        Queue<Text> queue = new ArrayDeque<>();
        collectSiblings(text, queue);
        return queue;
    }

    private static void collectSiblings(Text text, Queue<Text> queue) {
        queue.add(text);
        for (Text sibling : text.getSiblings()) {
            collectSiblings(sibling, queue);
        }
    }
}
