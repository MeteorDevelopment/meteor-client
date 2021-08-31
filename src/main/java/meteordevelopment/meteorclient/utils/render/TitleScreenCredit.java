/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.google.common.collect.Lists;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;

public class TitleScreenCredit {
    public final List<Word> words;
    public final int width;
    public final boolean isAddon;

    public TitleScreenCredit(String name, Color nameColor, List<String> authors, boolean isAddon) {
        this.words = getWords(name, nameColor, authors);
        this.isAddon = isAddon;

        int totalWidth = 0;
        for (Word word : words) totalWidth += word.width();
        width = totalWidth;
    }

    private List<Word> getWords(String name, Color nameColor, List<String> authors) {
        List<Word> words = new ArrayList<>();
        authors = Lists.reverse(authors);

        words.add(new Word(".", Utils.GRAY));

        for (String author : authors) {
            words.add(new Word(author, Utils.WHITE));

            String joiner = (authors.size() > 1 && author.equals(authors.get(0))) ? " & " : (author.equals(authors.get(authors.size() - 1))) ? "" : ", ";
            words.add(new Word(joiner, Utils.GRAY));
        }

        words.add(new Word(" by ", Utils.GRAY));
        words.add(new Word(name, nameColor));

        return words;
    }
}
