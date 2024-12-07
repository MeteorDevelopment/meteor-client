/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.commands.CreativeCommandHelper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class TransmogrifyCommand extends Command {
    public TransmogrifyCommand() {
        super("transmogrify", "", "tranmog");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item", RegistryKeyArgumentType.registryKey(RegistryKeys.ITEM)).executes(context -> {
            ItemStack stack = mc.player.getMainHandStack();
            CreativeCommandHelper.assertValid(stack);

            Identifier itemId = context.getArgument("item", RegistryKey.class).getValue();
            Item item = Registries.ITEM.get(itemId);
            stack.set(DataComponentTypes.ITEM_MODEL, itemId);
            stack.set(DataComponentTypes.ITEM_NAME, item.getName());

            CreativeCommandHelper.setStack(stack);

            return SINGLE_SUCCESS;
        }));
    }
}
