/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ContainerTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_CONTAINER_BACKGROUND = MeteorClient.identifier("textures/container.png");

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
    public int getHeight() {
        return 67;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 176;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {

        // Background
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        context.drawTexture(TEXTURE_CONTAINER_BACKGROUND, x, y, 0, 0, 0, 176, 67, 176, 67);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        //Contents
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
