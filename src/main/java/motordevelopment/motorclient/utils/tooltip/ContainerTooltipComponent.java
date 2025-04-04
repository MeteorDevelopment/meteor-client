/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.tooltip;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.utils.render.RenderUtils;
import motordevelopment.motorclient.utils.render.color.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ContainerTooltipComponent implements TooltipComponent, MotorTooltipData {
    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = MotorClient.identifier("textures/container.png");

    private final ItemStack[] items;
    private final Color color;

    public ContainerTooltipComponent(ItemStack[] items, Color color) {
        this.items = items;
        this.color = color;
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 67;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 176;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        // Background
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE_CONTAINER_BACKGROUND, x, y, 0, 0, 176, 67, 176, 67, color.getPacked());

        // Contents
        int row = 0;
        int i = 0;

        for (ItemStack itemStack : items) {
            RenderUtils.drawItem(context, itemStack, x + 8 + i * 18, y + 7 + row * 18, 1, true);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
    }
}
