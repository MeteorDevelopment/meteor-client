/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PeekCommand extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];
    private static final SimpleCommandExceptionType NOT_HOLDING_SHULKER_BOX =
            new SimpleCommandExceptionType(new LiteralText("You must be holding a shulker box."));

    public PeekCommand() {
        super("peek", "Lets you see what's inside shulker boxes.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ItemStack itemStack;
            if (Utils.isShulker(mc.player.getMainHandStack().getItem())) itemStack = mc.player.getMainHandStack();
            else if (Utils.isShulker(mc.player.getOffHandStack().getItem())) itemStack = mc.player.getOffHandStack();
            else throw NOT_HOLDING_SHULKER_BOX.create();

            Utils.getItemsInContainerItem(itemStack, ITEMS);
            MeteorClient.INSTANCE.screenToOpen = new PeekShulkerBoxScreen(new ShulkerBoxScreenHandler(0, mc.player.inventory, new SimpleInventory(ITEMS)), mc.player.inventory, itemStack.getName());

            return SINGLE_SUCCESS;
        });
    }

    public static class PeekShulkerBoxScreen extends ShulkerBoxScreen {
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
