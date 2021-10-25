/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TitleScreenCreditsRenderer {
    private static final int WHITE = Color.fromRGBA(255, 255, 255, 255);
    private static final int GRAY = Color.fromRGBA(175, 175, 175, 255);

    private static final List<Credit> credits = new ArrayList<>();

    private static void init() {
        add(AddonManager.METEOR);
        for (MeteorAddon addon : AddonManager.ADDONS) add(addon);

        // Sort by width (Meteor always first)
        credits.sort(Comparator.comparingInt(value -> value.sections.get(0).text.equals("Meteor Client ") ? Integer.MIN_VALUE : -value.width));
    }

    private static void add(MeteorAddon addon) {
        Credit credit = new Credit();

        credit.sections.add(new Section(addon.name + " ", addon.color.getPacked()));
        credit.sections.add(new Section("by ", GRAY));

        for (int i = 0; i < addon.authors.length; i++) {
            if (i > 0) {
                credit.sections.add(new Section(i == addon.authors.length - 1 ? " & " : ", ", GRAY));
            }

            credit.sections.add(new Section(addon.authors[i], WHITE));
        }

        credit.calculateWidth();
        credits.add(credit);
    }

    public static void render(MatrixStack matrices) {
        if (credits.isEmpty()) init();

        int y = 3;

        for (Credit credit : credits) {
            int x = mc.currentScreen.width - 3 - credit.width;

            for (Section section : credit.sections) {
                mc.textRenderer.drawWithShadow(matrices, section.text, x, y, section.color);
                x += section.width;
            }

            y += mc.textRenderer.fontHeight + 2;
        }
    }

    private static class Credit {
        public final List<Section> sections = new ArrayList<>();
        public int width;

        public void calculateWidth() {
            width = 0;
            for (Section section : sections) width += section.width;
        }
    }

    private static class Section {
        public final String text;
        public final int color, width;

        public Section(String text, int color) {
            this.text = text;
            this.color = color;
            this.width = mc.textRenderer.getWidth(text);
        }
    }
}
