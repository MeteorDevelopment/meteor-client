/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

public class GiveCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(Text.literal("You must be in creative mode to use this."));
    private final static SimpleCommandExceptionType NO_SPACE = new SimpleCommandExceptionType(Text.literal("No space in hotbar."));

    public GiveCommand() {
        super("give", "Gives you any item.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS)).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
            FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
            if (!fir.found()) throw NO_SPACE.create();

            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + fir.slot(), item));

            return SINGLE_SUCCESS;
        }).then(argument("number", IntegerArgumentType.integer()).executes(context -> {
            if (!mc.player.getAbilities().creativeMode) throw NOT_IN_CREATIVE.create();

            ItemStack item = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(IntegerArgumentType.getInteger(context, "number"), false);
            FindItemResult fir = InvUtils.find(ItemStack::isEmpty, 0, 8);
            if (!fir.found()) throw NO_SPACE.create();

            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36 + fir.slot(), item));

            return SINGLE_SUCCESS;
        })));
    }
}
