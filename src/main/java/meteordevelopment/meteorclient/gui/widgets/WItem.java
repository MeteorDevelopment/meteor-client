/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WItem extends WWidget {
    private static final MatrixStack MATRICES = new MatrixStack();

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
        if (!itemStack.isEmpty()) {
            renderer.post(() -> {
                double s = theme.scale(2);

                MATRICES.push();
                MATRICES.scale((float) s, (float) s, 1);
                MATRICES.translate(x / s, y / s, 0);

                mc.getItemRenderer().renderGuiItemIcon(MATRICES, itemStack, 0, 0);
                mc.getItemRenderer().renderGuiItemOverlay(MATRICES, mc.textRenderer, itemStack, 0, 0);

                MATRICES.pop();
            });
        }
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
    }
}
