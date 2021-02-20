/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class LookingAtHud extends DoubleTextHudElement {
    public LookingAtHud(HUD hud) {
        super(hud, "looking-at", "Displays what entity or block you are looking at.", "Looking At: ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) return "Obsidian";

        if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) return mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos()).getBlock().getName().getString();
        else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) return ((EntityHitResult) mc.crosshairTarget).getEntity().getDisplayName().getString();
        return "";
    }
}
