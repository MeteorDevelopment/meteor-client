/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PeekScreen extends ShulkerBoxScreen {
    private final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
    private final ItemStack storageBlock;

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxScreenHandler(0, mc.player.getInventory(), new SimpleInventory(contents)), mc.player.getInventory(), storageBlock.getName());
        this.storageBlock = storageBlock;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(false, button, 0) && focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            ItemStack itemStack = focusedSlot.getStack();
            return tooltips.openContent(itemStack);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(true, keyCode, modifiers) && focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            ItemStack itemStack = focusedSlot.getStack();
            if (tooltips.openContent(itemStack)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }

        return false;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        Color color = Utils.getShulkerColor(storageBlock);

        int i = (width - backgroundWidth) / 2;
        int j = (height - backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0f, 0f, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight, 256, 256, ColorHelper.fromFloats(color.a / 255f, color.r / 255f, color.g / 255f, color.b / 255f));
    }
}
