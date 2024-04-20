/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import net.minecraft.item.Item;


public class SectionVisibleEvent {
    private static final SectionVisibleEvent INSTANCE = new SectionVisibleEvent();

    public Item.TooltipContext section;
    public boolean visible;

    public static SectionVisibleEvent get(Item.TooltipContext section, boolean visible) {
        INSTANCE.section = section;
        INSTANCE.visible = visible;
        return INSTANCE;
    }
}
