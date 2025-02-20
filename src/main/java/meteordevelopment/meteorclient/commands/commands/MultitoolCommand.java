/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.commands.CreativeCommandHelper;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.item.BlockPredicatesChecker;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class MultitoolCommand extends Command {
    public MultitoolCommand() {
        super("multitool", "Makes your held item able to mine (almost) anything.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ItemStack stack = mc.player.getMainHandStack();
            CreativeCommandHelper.assertValid(stack);

            stack.set(DataComponentTypes.ITEM_MODEL, Identifier.of("meteor-client", "multitool"));
            stack.set(DataComponentTypes.ITEM_NAME, Text.literal("Multi Tool"));
            stack.set(DataComponentTypes.TOOL, new ToolComponent(List.of(
                ToolComponent.Rule.ofAlwaysDropping(Registries.BLOCK.getOrThrow(BlockTags.AXE_MINEABLE), 8f),
                ToolComponent.Rule.ofAlwaysDropping(Registries.BLOCK.getOrThrow(BlockTags.HOE_MINEABLE), 8f),
                ToolComponent.Rule.ofAlwaysDropping(Registries.BLOCK.getOrThrow(BlockTags.PICKAXE_MINEABLE), 8f),
                ToolComponent.Rule.ofAlwaysDropping(Registries.BLOCK.getOrThrow(BlockTags.SHOVEL_MINEABLE), 8f),
                ToolComponent.Rule.ofAlwaysDropping(Registries.BLOCK.getOrThrow(BlockTags.SWORD_EFFICIENT), 8f),
                ToolComponent.Rule.ofAlwaysDropping(RegistryEntryList.of(Registries.BLOCK.getEntry(Blocks.COBWEB)), 16f)
            ), 10f, 1));
            stack.set(DataComponentTypes.CAN_BREAK, new BlockPredicatesChecker(List.of(
                BlockPredicate.Builder.create().tag(Registries.BLOCK, BlockTags.AXE_MINEABLE).build(),
                BlockPredicate.Builder.create().tag(Registries.BLOCK, BlockTags.HOE_MINEABLE).build(),
                BlockPredicate.Builder.create().tag(Registries.BLOCK, BlockTags.PICKAXE_MINEABLE).build(),
                BlockPredicate.Builder.create().tag(Registries.BLOCK, BlockTags.SHOVEL_MINEABLE).build(),
                BlockPredicate.Builder.create().tag(Registries.BLOCK, BlockTags.SWORD_EFFICIENT).build()
            ), false));

            CreativeCommandHelper.setStack(stack);

            return SINGLE_SUCCESS;
        });
    }
}
