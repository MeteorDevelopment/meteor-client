/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.text;

import net.minecraft.text.PlainTextContent;
import net.minecraft.text.StringVisitable;
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
     * Collapses the tree of {@link Text} siblings into a one dimensional FIFO {@link Queue}. To match the behaviours of
     * the {@link Text#visit(StringVisitable.Visitor)} and {@link Text#visit(StringVisitable.StyledVisitor, Style)}
     * methods, texts with empty contents (created from {@link Text#empty()}) are ignored but their siblings are still
     * processed.
     * @param text the text
     * @return the text and its siblings in the order they appear when rendered.
     */
    static ArrayDeque<Text> collectSiblings(Text text) {
        ArrayDeque<Text> queue = new ArrayDeque<>();
        collectSiblings(text, queue);
        return queue;
    }

    private static void collectSiblings(Text text, Queue<Text> queue) {
        if (!(text.getContent() instanceof PlainTextContent ptc) || !ptc.string().isEmpty()) queue.add(text);
        for (Text sibling : text.getSiblings()) {
            collectSiblings(sibling, queue);
        }
    }
}
