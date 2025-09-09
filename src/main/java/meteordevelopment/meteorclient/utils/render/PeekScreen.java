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
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PeekScreen extends ShulkerBoxScreen {
    private final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
    private final ItemStack[] contents;
    private final ItemStack storageBlock;

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxScreenHandler(0, mc.player.getInventory(), new SimpleInventory(contents)), mc.player.getInventory(), storageBlock.getName());
        this.contents = contents;
        this.storageBlock = storageBlock;
    }

    @Override
    public boolean mouseClicked(Click arg, boolean bl) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        if (arg.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.currentScreenHandler.getCursorStack().isEmpty() && tooltips.middleClickOpen()) {
            ItemStack itemStack = focusedSlot.getStack();
            if (Utils.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
                return Utils.openContainer(focusedSlot.getStack(), contents, false);
            } else if (itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null || itemStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
                close();
                mc.setScreen(new BookScreen(BookScreen.Contents.create(itemStack)));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Click arg) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput arg) {
        if (arg.key() == GLFW.GLFW_KEY_ESCAPE || mc.options.inventoryKey.matchesKey(arg)) {
            close();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput arg) {
        if (arg.key() == GLFW.GLFW_KEY_ESCAPE) {
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
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0f, 0f, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight, 256, 256, ColorHelper.fromFloats(color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f));
    }
}
