/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.EntityAttributeArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.Registry;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class AttributeCommand extends Command {
    private final static SimpleCommandExceptionType NOT_IN_CREATIVE = new SimpleCommandExceptionType(new LiteralText("You must be in creative mode to use this."));
    private final static SimpleCommandExceptionType NOT_HOLDING_ITEM = new SimpleCommandExceptionType(new LiteralText("You need to hold some item to enchant."));


    public  AttributeCommand() {
        super("attribute", "Allows you to modify the item attributes of your held item. Requires creative mode.", "atr");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("one").then(argument("attribute", EntityAttributeArgumentType.attribute())
            .then(literal("level").then(argument("level", IntegerArgumentType.integer()).executes(context -> {
                one(context, context.getArgument("level", Integer.class));
                return SINGLE_SUCCESS;
            })))
            .then(literal("max").executes(context -> {
                ClampedEntityAttribute attribute = (ClampedEntityAttribute) context.getArgument("attribute", EntityAttribute.class);
                one(context, (int) attribute.getMaxValue());
                return SINGLE_SUCCESS;
            }))
        ));
        builder.then(literal("all")
            .then(literal("level").then(argument("level", IntegerArgumentType.integer()).executes(context -> {
                all(context.getArgument("level", Integer.class));
                return SINGLE_SUCCESS;
            })))
            .then(literal("max").executes(context -> {
                ClampedEntityAttribute attribute = (ClampedEntityAttribute) context.getArgument("attribute", EntityAttribute.class);
                one(context, (int) attribute.getMaxValue());
                return SINGLE_SUCCESS;
            }))
        );
        builder.then(literal("clear").executes(context -> {
            ItemStack itemStack = tryGetItemStack();
            Utils.clearAttributes(itemStack);

            syncItem();
            return SINGLE_SUCCESS;
        }));
    }

    private void one(CommandContext<CommandSource> context, int level) throws CommandSyntaxException{
        ItemStack itemStack = tryGetItemStack();

        EntityAttribute attribute = context.getArgument("attribute", EntityAttribute.class);
        Utils.addAttribute(itemStack, attribute, level);

        syncItem();
    }

    private void all(int level) throws CommandSyntaxException {
        ItemStack itemStack = tryGetItemStack();

        for (EntityAttribute attribute : Registry.ATTRIBUTE) {
            Utils.addAttribute(itemStack, attribute, level);
        }

        syncItem();
    }

    private void syncItem() {
        mc.setScreen(new InventoryScreen(mc.player));
        mc.setScreen(null);
    }

    private ItemStack tryGetItemStack() throws CommandSyntaxException {
        if (!mc.player.isCreative()) throw NOT_IN_CREATIVE.create();

        ItemStack itemStack = getItemStack();
        if (itemStack == null) throw NOT_HOLDING_ITEM.create();

        return itemStack;
    }

    private ItemStack getItemStack() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack == null) itemStack = mc.player.getOffHandStack();
        return itemStack.isEmpty() ? null : itemStack;
    }
}
