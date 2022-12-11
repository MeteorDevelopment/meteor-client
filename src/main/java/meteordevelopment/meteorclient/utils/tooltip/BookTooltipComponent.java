/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BookTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_BOOK_BACKGROUND = new Identifier("textures/gui/book.png");

    private final Text page;

    public BookTooltipComponent(Text page) {
        this.page = page;
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight() {
        return 134;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 112;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        // Background
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, TEXTURE_BOOK_BACKGROUND);
        DrawableHelper.drawTexture(matrices, x, y, z, 12, 0, 112, 134, 179, 179);

        // Content
        matrices.push();
        matrices.translate(x + 16, y + 12, z + 1);
        matrices.scale(0.7f, 0.7f, 1f);
        int offset = 0;
        for (OrderedText line : textRenderer.wrapLines(page, 112)) {
            textRenderer.draw(matrices, line, 0, offset, 0x000000);
            offset += 8;
        }
        matrices.pop();
    }
}
