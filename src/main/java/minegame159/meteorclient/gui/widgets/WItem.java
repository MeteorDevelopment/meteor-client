/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {
    public ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 32 * GuiConfig.get().guiScale;
        height = 32 * GuiConfig.get().guiScale;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.post(() -> {
            GlStateManager.enableTexture();
            DiffuseLighting.enable();
            GlStateManager.enableDepthTest();

            double s = GuiConfig.get().guiScale - 1;

            GlStateManager.pushMatrix();
            GlStateManager.scaled(2 + s, 2 + s, 1);
            GlStateManager.translated(x / (2 + s), y / (2 + s), 0);
            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(itemStack, 0, 0);
            GlStateManager.popMatrix();
        });
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
