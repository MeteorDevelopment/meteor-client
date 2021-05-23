/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;

@ElementRegister(name = "breaking-block")
public class BreakingBlockHud extends DoubleTextHudElement {
    public BreakingBlockHud() {
        super("breaking-block", "Displays percentage of the block you are breaking.", "Breaking Block: ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) return "0%";

        return String.format("%.0f%%", ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress() * 100);
    }
}
