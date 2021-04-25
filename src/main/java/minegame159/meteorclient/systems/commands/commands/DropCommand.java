/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DropCommand extends Command {
    private static final SimpleCommandExceptionType NOT_SPECTATOR = new SimpleCommandExceptionType(new LiteralText("Can't drop items while in spectator."));
    private static final DynamicCommandExceptionType NO_SUCH_ITEM = new DynamicCommandExceptionType(o -> new LiteralText("No such item " + o + "!"));

    public DropCommand() {
        super("drop", "Automatically drops specified items.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("hand").executes(context -> drop(player -> player.dropSelectedItem(true))))
                .then(literal("offhand").executes(context -> drop(player -> {
                    InvUtils.drop().slotOffhand();
                })))
                .then(literal("hotbar").executes(context -> drop(player -> {
                    for (int i = 0; i < 9; i++) {
                        InvUtils.drop().slotHotbar(i);
                    }
                })))
                .then(literal("inventory").executes(context -> drop(player -> {
                    for (int i = 9; i < player.inventory.main.size(); i++) {
                        InvUtils.drop().slotMain(i - 9);
                    }
                })))
                .then(literal("all").executes(context -> drop(player -> {
                    for (int i = 0; i < player.inventory.size(); i++) {
                        InvUtils.drop().slot(i);
                    }
                })))
                .then(literal("armor").executes(context -> drop(player -> {
                    for (int i = 0; i < player.inventory.armor.size(); i++) {
                        InvUtils.drop().slotArmor(i);
                    }
                })))
                .then(argument("item", StringArgumentType.string()).executes(context -> drop(player -> {
                    String itemName = context.getArgument("item", String.class);
                    Item item = Registry.ITEM.get(new Identifier(itemName.toLowerCase()));
                    if (item == Items.AIR) throw NO_SUCH_ITEM.create(itemName);

                    for (int i = 0; i < player.inventory.main.size(); i++) {
                        ItemStack itemStack = player.inventory.main.get(i);
                        if (itemStack.getItem() == item) InvUtils.drop().slot(i);
                    }
                })));
    }

    private int drop(PlayerConsumer consumer) throws CommandSyntaxException {
        ClientPlayerEntity player = mc.player;
        assert player != null;

        if (player.isSpectator()) throw NOT_SPECTATOR.create();

        consumer.accept(player);
        return SINGLE_SUCCESS;
    }

    // Separate interface so exceptions can be thrown from it (which is not the case for Consumer)
    @FunctionalInterface
    private interface PlayerConsumer {
        void accept(ClientPlayerEntity player) throws CommandSyntaxException;
    }

}
