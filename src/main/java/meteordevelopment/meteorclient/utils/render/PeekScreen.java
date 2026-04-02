/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PeekScreen extends ShulkerBoxScreen {
    private final Identifier TEXTURE = Identifier.parse("textures/gui/container/shulker_box.png");
    private final ItemStack storageBlock;

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxMenu(0, mc.player.getInventory(), new SimpleContainer(contents)), mc.player.getInventory(), storageBlock.getHoverName());
        this.storageBlock = storageBlock;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(click) && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && mc.player.containerMenu.getCarried().isEmpty()) {
            ItemStack itemStack = hoveredSlot.getItem();
            return tooltips.openContent(itemStack);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (tooltips.shouldOpenContents(input) && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && mc.player.containerMenu.getCarried().isEmpty()) {
            ItemStack itemStack = hoveredSlot.getItem();
            if (tooltips.openContent(itemStack)) {
                return true;
            }
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE || mc.options.keyInventory.matches(input)) {
            onClose();
            return true;
        }

        return false;
    }

    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        Color color = Utils.getShulkerColor(storageBlock);

        int i = (width - imageWidth) / 2;
        int j = (height - imageHeight) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight, 256, 256, ARGB.colorFromFloat(color.a / 255f, color.r / 255f, color.g / 255f, color.b / 255f));
    }
}
