/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {
    protected ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    protected void onCalculateSize() {
        double s = theme.scale(32);

        width = s;
        height = s;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.post(() -> {
            GlStateManager.enableTexture();
            DiffuseLighting.enable();
            GlStateManager.enableDepthTest();

            double s = theme.scale(2);

            GlStateManager.pushMatrix();
            GlStateManager.scaled(s, s, 1);
            GlStateManager.translated(x / s, y / s, 0);
            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(itemStack, 0, 0);
            GlStateManager.popMatrix();
        });
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
