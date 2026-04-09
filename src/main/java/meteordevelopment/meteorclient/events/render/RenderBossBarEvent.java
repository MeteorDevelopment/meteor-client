/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

import java.util.Iterator;

public class RenderBossBarEvent {
    public static class BossText {
        private static final BossText INSTANCE = new BossText();

        public LerpingBossEvent bossBar;
        public Component name;

        public static BossText get(LerpingBossEvent bossBar, Component name) {
            INSTANCE.bossBar = bossBar;
            INSTANCE.name = name;
            return INSTANCE;
        }
    }

    public static class BossSpacing {
        private static final BossSpacing INSTANCE = new BossSpacing();

        public int spacing;

        public static BossSpacing get(int spacing) {
            INSTANCE.spacing = spacing;
            return INSTANCE;
        }
    }

    public static class BossIterator {
        private static final BossIterator INSTANCE = new BossIterator();

        public Iterator<LerpingBossEvent> iterator;

        public static BossIterator get(Iterator<LerpingBossEvent> iterator) {
            INSTANCE.iterator = iterator;
            return INSTANCE;
        }
    }
}
