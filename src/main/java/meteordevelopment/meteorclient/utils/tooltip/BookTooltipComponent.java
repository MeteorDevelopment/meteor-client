/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;

public class BookTooltipComponent implements TooltipComponent, MeteorTooltipData {

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
        return 115;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 112;
    }

    @Override
    public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, VertexConsumerProvider.Immediate immediate) {
        //Content
        int offset = 0;
        for (OrderedText line : textRenderer.wrapLines(page, 112)) {
            textRenderer.draw(line, x, y+offset, 0xFFFFFF, true, matrix4f, immediate, true, 0x000000, 0xFFFFFF);
            offset += 8;
        }
    }
}
