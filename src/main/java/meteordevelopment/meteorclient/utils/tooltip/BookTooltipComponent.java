/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BookTooltipComponent implements TooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_BOOK_BACKGROUND = Identifier.of("textures/gui/book.png");

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
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        // Background
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        context.drawTexture(TEXTURE_BOOK_BACKGROUND, x, y, 0, 12, 0, 112, 134, 179, 179);

        // Content
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x + 16, y + 12, 1);
        matrices.scale(0.7f, 0.7f, 1f);
        int offset = 0;
        for (OrderedText line : textRenderer.wrapLines(page, 112)) {
            context.drawText(textRenderer, line, 0, offset, 0x000000, false);
            offset += 8;
        }
        matrices.pop();
    }
}
