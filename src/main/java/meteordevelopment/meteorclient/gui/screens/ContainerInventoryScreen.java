/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/*
 * i couldn't figure out how to add proper outer borders for the GUI without adding custom textures. @TODO
 */
public class ContainerInventoryScreen extends Screen {
    private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
    private static final int SLOT_SIZE = 18;
    private static final int SCREEN_WIDTH = 176;

    private final List<ItemStack> containerItems;
    private final PlayerInventory playerInventory;
    private final int containerRows;
    private int x, y;

    private int baseX, baseY;
    private int playerY;

    public ContainerInventoryScreen(ItemStack containerItem) {
        super(containerItem.getName());
        this.playerInventory = mc.player.getInventory();

        this.containerItems = new ArrayList<>();
        if (containerItem.getItem() instanceof BundleItem) {
            BundleContentsComponent bundleContents = containerItem.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                bundleContents.iterate().forEach(containerItems::add);
            }
        } else {
            ItemStack[] tempItems = new ItemStack[64];
            Utils.getItemsInContainerItem(containerItem, tempItems);
            Collections.addAll(containerItems, tempItems);
        }

        this.containerRows = Math.max(1, MathHelper.ceilDiv(containerItems.size(), 9));
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - SCREEN_WIDTH) / 2;
        this.y = (this.height - (114 + containerRows * SLOT_SIZE + 20)) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        baseX = x + 8;
        baseY = y + 18;
        playerY = baseY + containerRows * SLOT_SIZE + 20;

        // drawing the slot textures
        for (int row = 0; row < containerRows + 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotY = row < containerRows ? baseY + row * SLOT_SIZE : playerY + (row - containerRows) * SLOT_SIZE;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, baseX + col * SLOT_SIZE, slotY, SLOT_SIZE, SLOT_SIZE);
            }
        }

        // drawing the container items
        for (int i = 0; i < containerItems.size(); i++) {
            ItemStack item = containerItems.get(i);
            if (!item.isEmpty()) {
                int itemX = baseX + (i % 9) * SLOT_SIZE + 1;
                int itemY = baseY + (i / 9) * SLOT_SIZE + 1;
                context.drawItem(item, itemX, itemY);
                context.drawStackOverlay(textRenderer, item, itemX, itemY);
            }
        }

        // drawing your inventory items
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row < 3 ? 9 + row * 9 + col : col;
                ItemStack item = playerInventory.getStack(slotIndex);
                if (!item.isEmpty()) {
                    int itemX = baseX + col * SLOT_SIZE + 1;
                    int itemY = playerY + row * SLOT_SIZE + 1;
                    context.drawItem(item, itemX, itemY);
                    context.drawStackOverlay(textRenderer, item, itemX, itemY);
                }
            }
        }

        // drawing title headers
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) x, (float) y);
        if (textRenderer != null) {
            context.drawText(textRenderer, title, 8, 6, -12566464, false);
            context.drawText(textRenderer, playerInventory.getDisplayName(), 8, 18 + containerRows * SLOT_SIZE + 10, -12566464, false);
        }
        context.getMatrices().popMatrix();

        // drawing the tooltip
        ItemStack item = getSelectedItem(mouseX, mouseY);
        if (!item.isEmpty()) {
            context.drawTooltip(textRenderer, getTooltipFromItem(mc, item), item.getTooltipData(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        ItemStack stack = getSelectedItem((int) mouseX, (int) mouseY);
        if (tooltips.shouldOpenContents(false, button, 0)) {
            return tooltips.openContent(stack);
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);

        ItemStack stack = getSelectedItem((int) mc.mouse.getScaledX(mc.getWindow()), (int) mc.mouse.getScaledY(mc.getWindow()));
        if (tooltips.shouldOpenContents(true, keyCode, modifiers)) {
            return tooltips.openContent(stack);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            close();
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
            return playerInventory.getStack(slotIndex);
        }

        return ItemStack.EMPTY;
    }
}
