/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {
    public ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 32 * GuiConfig.INSTANCE.guiScale;
        height = 32 * GuiConfig.INSTANCE.guiScale;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.post(() -> {
            GlStateManager.enableTexture();
            DiffuseLighting.enable();
            GlStateManager.enableDepthTest();

            Window window = MinecraftClient.getInstance().getWindow();
            double s = window.getScaleFactor();
            double ss = Math.max(1, s - 1);

            double sg = GuiConfig.INSTANCE.guiScale;

            GlStateManager.pushMatrix();
            GlStateManager.translated(-x * ss * sg, -y * ss * sg, 0);
            GlStateManager.scaled(1 + sg, 1 + sg, 1);
            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(itemStack, (int) x, (int) y);
            GlStateManager.popMatrix();
        });
    }
}
