/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PeekScreen extends ShulkerBoxScreen {
    private final Identifier TEXTURE = new Identifier("textures/gui/container/shulker_box.png");
    private final ItemStack[] contents;
    private final ItemStack storageBlock;

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxScreenHandler(0, mc.player.getInventory(), new SimpleInventory(contents)), mc.player.getInventory(), storageBlock.getName());
        this.contents = contents;
        this.storageBlock = storageBlock;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        BetterTooltips toolips = Modules.get().get(BetterTooltips.class);

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty() && toolips.middleClickOpen()) {
            return Utils.openContainer(focusedSlot.getStack(), contents, false);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        Color color = Utils.getShulkerColor(storageBlock);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}
