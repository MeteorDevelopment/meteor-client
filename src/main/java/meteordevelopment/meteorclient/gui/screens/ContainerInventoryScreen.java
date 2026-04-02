/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/*
 * i couldn't figure out how to add proper outer borders for the GUI without adding custom textures.
 */
public class ContainerInventoryScreen extends Screen {
    private static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");
    private static final int SLOT_SIZE = 18;
    private static final int SCREEN_WIDTH = 176;

    private final List<ItemStack> containerItems;
    private final Inventory playerInventory;
    private final int containerRows;
    private int x, y;

    private int baseX, baseY;
    private int playerY;

    public ContainerInventoryScreen(ItemStack containerItem) {
        super(containerItem.getHoverName());
        this.playerInventory = mc.player.getInventory();

        this.containerItems = new ArrayList<>();
        if (containerItem.getItem() instanceof BundleItem) {
            BundleContents bundleContents = containerItem.get(DataComponents.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                bundleContents.items().forEach(containerItems::add);
            }
        } else {
            ItemStack[] tempItems = new ItemStack[64];
            Utils.getItemsInContainerItem(containerItem, tempItems);
            Collections.addAll(containerItems, tempItems);
        }

        this.containerRows = Math.max(1, Mth.positiveCeilDiv(containerItems.size(), 9));
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - SCREEN_WIDTH) / 2;
        this.y = (this.height - (114 + containerRows * SLOT_SIZE + 20)) / 2;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        baseX = x + 8;
        baseY = y + 18;
        playerY = baseY + containerRows * SLOT_SIZE + 20;

        // drawing the slot textures
        for (int row = 0; row < containerRows + 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotY = row < containerRows ? baseY + row * SLOT_SIZE : playerY + (row - containerRows) * SLOT_SIZE;
                context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, baseX + col * SLOT_SIZE, slotY, SLOT_SIZE, SLOT_SIZE);
            }
        }

        // drawing the container items
        for (int i = 0; i < containerItems.size(); i++) {
            ItemStack item = containerItems.get(i);
            if (!item.isEmpty()) {
                int itemX = baseX + (i % 9) * SLOT_SIZE + 1;
                int itemY = baseY + (i / 9) * SLOT_SIZE + 1;
                context.renderItem(item, itemX, itemY);
                context.renderItemDecorations(font, item, itemX, itemY);
            }
        }

        // drawing your inventory items
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row < 3 ? 9 + row * 9 + col : col;
                ItemStack item = playerInventory.getItem(slotIndex);
                if (!item.isEmpty()) {
                    int itemX = baseX + col * SLOT_SIZE + 1;
                    int itemY = playerY + row * SLOT_SIZE + 1;
                    context.renderItem(item, itemX, itemY);
                    context.renderItemDecorations(font, item, itemX, itemY);
                }
            }
        }

        // drawing title headers
        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        if (font != null) {
            context.drawString(font, title, 8, 6, -12566464, false);
            context.drawString(font, playerInventory.getDisplayName(), 8, 18 + containerRows * SLOT_SIZE + 10, -12566464, false);
        }
        context.pose().popMatrix();

        // drawing the tooltip
        ItemStack item = getSelectedItem(mouseX, mouseY);
        if (!item.isEmpty()) {
            context.setTooltipForNextFrame(font, getTooltipFromItem(mc, item), item.getTooltipImage(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        ItemStack stack = getSelectedItem((int) click.x(), (int) click.y());
        if (tooltips.shouldOpenContents(click)) {
            return tooltips.openContent(stack);
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        ItemStack stack = getSelectedItem((int) mc.mouseHandler.getScaledXPos(mc.getWindow()), (int) mc.mouseHandler.getScaledYPos(mc.getWindow()));
        if (tooltips.shouldOpenContents(input)) {
            return tooltips.openContent(stack);
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE || mc.options.keyInventory.matches(input)) {
            onClose();
            return true;
        }

        return false;
    }

    private ItemStack getSelectedItem(int mouseX, int mouseY) {
        if (mouseX < baseX || mouseX > baseX + 9 * SLOT_SIZE) return ItemStack.EMPTY;

        int col = (mouseX - baseX) / SLOT_SIZE;
        if (col > 8) return ItemStack.EMPTY;

        if (mouseY >= baseY && mouseY < baseY + containerRows * SLOT_SIZE) {
            int index = ((mouseY - baseY) / SLOT_SIZE) * 9 + col;
            return (index < containerItems.size() ? containerItems.get(index) : ItemStack.EMPTY);
        }

        if (mouseY >= playerY && mouseY < playerY + 4 * SLOT_SIZE) {
            int row = (mouseY - playerY) / SLOT_SIZE;
            int slotIndex = row < 3 ? 9 + row * 9 + col : col;
            return playerInventory.getItem(slotIndex);
        }

        return ItemStack.EMPTY;
    }
}
