/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.ArrayList;
import java.util.List;

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
    private final Item.TooltipContext tooltipContext;
    private int x, y;

    public ContainerInventoryScreen(ItemStack containerItem) {
        super(containerItem.getName());
        this.playerInventory = mc.player.getInventory();
        this.tooltipContext = Item.TooltipContext.create(mc.world);
        
        this.containerItems = new ArrayList<>();
        if (containerItem.getItem() instanceof BundleItem) {
            BundleContentsComponent bundleContents = containerItem.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContents != null) {
                bundleContents.iterate().forEach(containerItems::add);
            }
        } else {
            ItemStack[] tempItems = new ItemStack[64];
            Utils.getItemsInContainerItem(containerItem, tempItems);
            for (ItemStack stack : tempItems) {
                if (stack != null && !stack.isEmpty()) {
                    containerItems.add(stack);
                }
            }
        }
        
        this.containerRows = Math.max(1, (containerItems.size() + 8) / 9);
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
        
        int baseX = x + 8;
        int baseY = y + 18;
        int playerY = baseY + containerRows * SLOT_SIZE + 20;
        
        for (int row = 0; row < containerRows + 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotY = row < containerRows ? baseY + row * SLOT_SIZE : playerY + (row - containerRows) * SLOT_SIZE;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, baseX + col * SLOT_SIZE, slotY, SLOT_SIZE, SLOT_SIZE);
            }
        }
        
        for (int i = 0; i < containerItems.size(); i++) {
            ItemStack item = containerItems.get(i);
            if (!item.isEmpty()) {
                int itemX = baseX + (i % 9) * SLOT_SIZE + 1;
                int itemY = baseY + (i / 9) * SLOT_SIZE + 1;
                context.drawItem(item, itemX, itemY);
                context.drawStackOverlay(textRenderer, item, itemX, itemY);
            }
        }
        
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
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float)x, (float)y);
        if (textRenderer != null) {
            context.drawText(textRenderer, title != null && !title.getString().isEmpty() ? title : Text.literal("Container"), 8, 6, -12566464, false);
            context.drawText(textRenderer, playerInventory.getDisplayName(), 8, 18 + containerRows * SLOT_SIZE + 10, -12566464, false);
        }
        context.getMatrices().popMatrix();
        
        if (mouseX >= baseX && mouseX < baseX + 9 * SLOT_SIZE) {
            int col = (mouseX - baseX) / SLOT_SIZE;
            
            if (mouseY >= baseY && mouseY < baseY + containerRows * SLOT_SIZE) {
                int index = ((mouseY - baseY) / SLOT_SIZE) * 9 + col;
                if (index < containerItems.size()) {
                    ItemStack item = containerItems.get(index);
                    if (!item.isEmpty()) {
                        context.drawTooltip(textRenderer, item.getTooltip(tooltipContext, mc.player, TooltipType.BASIC), item.getTooltipData(), mouseX, mouseY);
                    }
                }
            } else if (mouseY >= playerY && mouseY < playerY + 4 * SLOT_SIZE) {
                int row = (mouseY - playerY) / SLOT_SIZE;
                int slotIndex = row < 3 ? 9 + row * 9 + col : col;
                if (slotIndex < playerInventory.size()) {
                    ItemStack item = playerInventory.getStack(slotIndex);
                    if (!item.isEmpty()) {
                        context.drawTooltip(textRenderer, item.getTooltip(tooltipContext, mc.player, TooltipType.BASIC), item.getTooltipData(), mouseX, mouseY);
                    }
                }
            }
        }
    }

}
