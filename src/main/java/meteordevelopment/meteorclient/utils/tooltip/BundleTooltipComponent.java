/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.List;

public class BundleTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
    private static final Identifier BUNDLE_SLOT_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final Identifier BUNDLE_PROGRESS_BAR_BORDER_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
    private static final Identifier BUNDLE_PROGRESS_BAR_FILL_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
    private static final Identifier BUNDLE_PROGRESS_BAR_FULL_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
    private static final Identifier BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final Identifier BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");

    private static final int SLOTS_PER_ROW = 8;
    private static final int SLOT_DIMENSION = 24;
    private static final int ROW_WIDTH = 8 + SLOTS_PER_ROW * SLOT_DIMENSION + 8;
    private static final int PROGRESS_BAR_WIDTH = 94;
    private static final int PROGRESS_BAR_HEIGHT = 13;
    private static final Component BUNDLE_FULL = Component.translatable("item.minecraft.bundle.full");

    private final ItemStack[] items;
    private final BundleContents bundleContents;
    private final int width;
    private final int height;

    public BundleTooltipComponent(ItemStack[] items, BundleContents bundleContents) {
        this.items = items;
        this.bundleContents = bundleContents;

        int rows = (items.length + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
        this.width = ROW_WIDTH;
        this.height = 8 + rows * SLOT_DIMENSION + 8 + PROGRESS_BAR_HEIGHT + 4;
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(Font textRenderer) {
        return height;
    }

    @Override
    public int getWidth(Font textRenderer) {
        return width;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, int width, int height, GuiGraphics context) {
        int row = 0;
        int col = 0;

        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) {
                int slotX = x + 8 + col * SLOT_DIMENSION;
                int slotY = y + 8 + row * SLOT_DIMENSION;

                context.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_BACKGROUND_TEXTURE, slotX, slotY, SLOT_DIMENSION, SLOT_DIMENSION);
                drawItem(itemStack, (row * 8) + col, slotX, slotY, textRenderer, context);
                context.renderItemDecorations(textRenderer, itemStack, slotX + 4, slotY + 4);
            }

            col++;
            if (col >= SLOTS_PER_ROW) {
                col = 0;
                row++;
            }
        }

        drawSelectedItemTooltip(textRenderer, context, x, y, width);

        int progressBarX = x + (this.width - PROGRESS_BAR_WIDTH) / 2;
        int progressBarY = y + this.height - PROGRESS_BAR_HEIGHT - 4;
        drawProgressBar(progressBarX, progressBarY, textRenderer, context);
    }

    private void drawItem(ItemStack itemStack, int index, int x, int y, Font textRenderer, GuiGraphics drawContext) {
        boolean bl = bundleContents.getSelectedItem() == index;
        if (bl) {
            drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE, x, y, 24, 24);
        } else {
            drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_BACKGROUND_TEXTURE, x, y, 24, 24);
        }

        drawContext.renderItem(itemStack, x + 4, y + 4, 0);
        drawContext.renderItemDecorations(textRenderer, itemStack, x + 4, y + 4);
        if (bl) {
            drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE, x, y, 24, 24);
        }
    }

    private void drawSelectedItemTooltip(Font textRenderer, GuiGraphics drawContext, int x, int y, int width) {
        if (this.bundleContents.hasSelectedItem()) {
            ItemStack itemStack = this.bundleContents.getItemUnsafe(this.bundleContents.getSelectedItem());
            Component text = itemStack.getStyledHoverName();
            int i = textRenderer.width(text.getVisualOrderText());
            int j = x + width / 2 - 12;
            ClientTooltipComponent tooltipComponent = ClientTooltipComponent.create(text.getVisualOrderText());
            drawContext.renderTooltip(
                textRenderer, List.of(tooltipComponent), j - i / 2, y - 37, DefaultTooltipPositioner.INSTANCE, itemStack.get(DataComponents.TOOLTIP_STYLE)
            );
        }
    }

    private void drawProgressBar(int x, int y, Font textRenderer, GuiGraphics context) {
        int fillAmount = Mth.clamp(Mth.mulAndTruncate(bundleContents.weight(), PROGRESS_BAR_WIDTH), 0, PROGRESS_BAR_WIDTH);

        Identifier fillTexture = bundleContents.weight().compareTo(Fraction.ONE) >= 0
            ? BUNDLE_PROGRESS_BAR_FULL_TEXTURE
            : BUNDLE_PROGRESS_BAR_FILL_TEXTURE;

        context.blitSprite(RenderPipelines.GUI_TEXTURED, fillTexture, x + 1, y, fillAmount, PROGRESS_BAR_HEIGHT);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_PROGRESS_BAR_BORDER_TEXTURE, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);

        Component label = getProgressBarLabel();
        if (label != null) {
            context.drawCenteredString(textRenderer, label, x + PROGRESS_BAR_WIDTH / 2, y + 3, CommonColors.WHITE);
        }
    }

    private Component getProgressBarLabel() {
        return bundleContents.weight().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL : Component.literal(String.format("%.2f%%", bundleContents.weight().floatValue() * 100));
    }
}
