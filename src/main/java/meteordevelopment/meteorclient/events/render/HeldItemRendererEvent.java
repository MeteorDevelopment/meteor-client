/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class HeldItemRendererEvent {
    private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();

    public Hand hand;
    public MatrixStack matrix;

    public static HeldItemRendererEvent get(Hand hand, MatrixStack matrices) {
        INSTANCE.hand = hand;
        INSTANCE.matrix = matrices;
        return INSTANCE;
    }
}
