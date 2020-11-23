/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;

public class Peek extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];

    public Peek() {
        super("peek", "Lets you see whats inside shulker boxes.");
    }

    @Override
    public void run(String[] args) {
        PlayerEntity player = MinecraftClient.getInstance().player;

        ItemStack itemStack;
        if (Utils.isShulker(player.getMainHandStack().getItem())) itemStack = player.getMainHandStack();
        else if (Utils.isShulker(player.getOffHandStack().getItem())) itemStack = player.getOffHandStack();
        else {
            Chat.error("You must be holding a shulker box.");
            return;
        }

        Utils.getItemsInContainerItem(itemStack, ITEMS);
        MeteorClient.INSTANCE.screenToOpen = new PeekShulkerBoxScreen(new ShulkerBoxScreenHandler(0, player.inventory, new SimpleInventory(ITEMS)), player.inventory, itemStack.getName());
    }

    private static class PeekShulkerBoxScreen extends ShulkerBoxScreen {
        public PeekShulkerBoxScreen(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
            super(handler, inventory, title);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}
