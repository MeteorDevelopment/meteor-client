/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;

public class DurabilityHud extends DoubleTextHudElement {
    public DurabilityHud(HUD hud) {
        super(hud, "durability", "Displays durability of the item you are holding.", "Durability: ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) return "159";

        Integer amount = null;
        if (!mc.player.getMainHandStack().isEmpty() && mc.player.getMainHandStack().isDamageable()) amount = mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage();

        return amount == null ? "" : amount.toString();
    }
}
