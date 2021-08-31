/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.TitleScreenCredit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CreditsManager {
    public static final List<TitleScreenCredit> CREDITS = new ArrayList<>();

    public static void init() {
        CREDITS.add(new TitleScreenCredit("Meteor Client", Utils.METEOR, List.of("MineGame159", "seasnail", "squidoodly"), false));

        for (MeteorAddon addon : MeteorClient.ADDONS) {
            CREDITS.add(new TitleScreenCredit(addon.name, addon.color, addon.authors, true));
        }

        CREDITS.sort(Comparator.comparingInt(credit -> -credit.width));
    }
}
