/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Drop extends Command {
    public Drop() {
        super("drop", "Drops things.");
    }

    @Override
    public void run(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player.isSpectator()) {
            Chat.error("Can't drop items while in spectator.");
            return;
        }

        if (args.length == 0) {
            sendErrorMessage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "hand":
                mc.player.dropSelectedItem(true);
                break;
            case "offhand":
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(InvUtils.OFFHAND_SLOT), 1, SlotActionType.THROW);
                break;
            case "hotbar":
                for (int i = 0; i < 9; i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            case "inventory":
                for (int i = 9; i < mc.player.inventory.main.size(); i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            case "all":
                for (int i = 0; i < mc.player.inventory.main.size(); i++) {
                    InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                }
                break;
            default:
                Item item = Registry.ITEM.get(new Identifier("minecraft", args[0].toLowerCase()));
                if (item == Items.AIR) sendErrorMessage();
                else {
                    for (int i = 0; i < mc.player.inventory.main.size(); i++) {
                        ItemStack itemStack = mc.player.inventory.main.get(i);
                        if (itemStack.getItem() == item) InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                    }
                }
                break;
        }
    }

    private void sendErrorMessage() {
        Chat.error("You need to select a mode. (hand, offhand, hotbar, inventory, all, <item_name>)");
    }
}
