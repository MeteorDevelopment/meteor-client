/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import net.minecraft.component.DataComponentType;


public class SectionVisibleEvent {
    private static final SectionVisibleEvent INSTANCE = new SectionVisibleEvent();

    public DataComponentType<?> section;
    public boolean visible;

    public static SectionVisibleEvent get(DataComponentType<?> section, boolean visible) {
        INSTANCE.section = section;
        INSTANCE.visible = visible;
        return INSTANCE;
    }
}
