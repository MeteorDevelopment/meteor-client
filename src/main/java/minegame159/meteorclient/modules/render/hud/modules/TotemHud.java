/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemHud extends HudModule {
    public TotemHud(HUD hud) { super(hud, "totems", "Displays the amount of totems in your inventory."); }



    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * hud.totemCountScale.get(), 16 * hud.totemCountScale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX() / hud.totemCountScale.get();
        double y = box.getY() / hud.totemCountScale.get();

        if(mc.player == null || mc.currentScreen instanceof HudEditorScreen) {
            RenderUtils.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), (int) x, (int) y, hud.totemCountScale.get(), true);
        } else if(InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING).count > 0) {
            RenderUtils.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING, InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING).count), (int) x, (int) y, hud.totemCountScale.get(), true);
        }
    }
}
