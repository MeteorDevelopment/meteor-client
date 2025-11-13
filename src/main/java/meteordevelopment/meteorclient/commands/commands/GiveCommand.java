/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(MeteorClient.translatable("meteor.command.give.exception.not_in_creative"));
    private final static SimpleCommandExceptionType NO_SPACE = new SimpleCommandExceptionType(MeteorClient.translatable("meteor.command.give.exception.no_space"));

    public GiveCommand() {
        super("give");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS)).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
            giveItem(item);

            return SINGLE_SUCCESS;
        }).then(argument("number", IntegerArgumentType.integer(1, 99)).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), true);
            giveItem(item);

            return SINGLE_SUCCESS;
        })));
    }

    private void giveItem(ItemStack item) throws CommandSyntaxException {
        FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
        if (!fir.found()) throw NO_SPACE.create();

        mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + fir.slot(), item));
        mc.player.playerScreenHandler.getSlot(36 + fir.slot()).setStack(item);
    }
}
