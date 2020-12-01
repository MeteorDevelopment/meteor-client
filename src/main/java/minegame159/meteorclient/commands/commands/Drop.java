/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Drop extends Command {
    private static final SimpleCommandExceptionType NOT_SPECTATOR =
            new SimpleCommandExceptionType(new LiteralText("Can't drop items while in spectator."));
    private static final DynamicCommandExceptionType NO_SUCH_ITEM =
            new DynamicCommandExceptionType(o -> new LiteralText("No such item " + o + "!"));

    public Drop() {
        super("drop", "Drops things.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("hand").executes(context -> drop(player -> player.dropSelectedItem(true))))
                .then(literal("offhand").executes(context -> drop(player -> InvUtils.clickSlot(InvUtils.invIndexToSlotId(InvUtils.OFFHAND_SLOT), 1, SlotActionType.THROW))))
                .then(literal("hotbar").executes(context -> drop(player -> {
                    for (int i = 0; i < 9; i++) {
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                    }
                })))
                .then(literal("inventory").executes(context -> drop(player -> {
                    for (int i = 9; i < player.inventory.main.size(); i++) {
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                    }
                })))
                .then(literal("all").executes(context -> drop(player -> {
                    for (int i = 0; i < player.inventory.main.size(); i++) {
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                    }
                })))
                .then(argument("item", StringArgumentType.string()).executes(context -> drop(player -> {
                    String itemName = context.getArgument("item", String.class);
                    Item item = Registry.ITEM.get(new Identifier(itemName.toLowerCase()));
                    if (item == Items.AIR) throw NO_SUCH_ITEM.create(itemName);

                    for (int i = 0; i < player.inventory.main.size(); i++) {
                        ItemStack itemStack = player.inventory.main.get(i);
                        if (itemStack.getItem() == item)
                            InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
                    }
                })));
    }

    private int drop(PlayerConsumer consumer) throws CommandSyntaxException {
        ClientPlayerEntity player = MC.player;
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
