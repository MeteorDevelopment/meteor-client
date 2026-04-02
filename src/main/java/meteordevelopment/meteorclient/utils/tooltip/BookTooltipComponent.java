/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

public class BookTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
    private static final Identifier TEXTURE_BOOK_BACKGROUND = Identifier.parse("textures/gui/book.png");

    private final Component page;

    public BookTooltipComponent(Component page) {
        this.page = page;
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(Font textRenderer) {
        return 134;
    }

    @Override
    public int getWidth(Font textRenderer) {
        return 112;
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, int width, int height, GuiGraphics context) {
        // Background
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_BOOK_BACKGROUND, x - 10, y, 0, 0, 128, 128, 179, 179);

        // Content
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x + 16, y + 12);
        matrices.scale(0.7f, 0.7f);
        int offset = 0;
        for (FormattedCharSequence line : textRenderer.split(page, 112)) {
            context.drawString(textRenderer, line, 0, offset, 0xFF000000, false);
            offset += 8;
        }
        matrices.popMatrix();
    }
}
