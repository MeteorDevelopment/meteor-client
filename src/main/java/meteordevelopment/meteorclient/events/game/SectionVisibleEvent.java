/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import net.minecraft.item.ItemStack;

public class SectionVisibleEvent {
    private static final SectionVisibleEvent INSTANCE = new SectionVisibleEvent();

    public ItemStack.TooltipSection section;
    public boolean visible;

    public static SectionVisibleEvent get(ItemStack.TooltipSection section, boolean visible) {
        INSTANCE.section = section;
        INSTANCE.visible = visible;
        return INSTANCE;
    }
}
