/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.systems.modules.render.ItemHighlight;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemCommand extends Command {
    final InventoryTweaks inventoryTweaks;
    final ItemHighlight itemHighlight;

    public ItemCommand() {
        super("item", "Manages item related modules.");

        inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        itemHighlight = Modules.get().get(ItemHighlight.class);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        buildHighlight(builder);
        buildBlock(builder);
        buildLock(builder);
    }

    private void buildHighlight(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("highlight")
            .executes(context -> {
                List<Item> items = itemHighlight.items.get();
                ItemStack itemStack = getItemStack();

                if (itemStack == null) {
                    error("Not holding an item.");

                    return SINGLE_SUCCESS;
                }

                Item item = itemStack.getItem();

                if (items.contains(item)) return SINGLE_SUCCESS;

                items.add(item);

                itemHighlight.items.set(items);
                itemHighlight.info("Added " + item.toString() + " to highlighted items.");

                return SINGLE_SUCCESS;
            })
            .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                .executes(context -> {
                    List<Item> items = itemHighlight.items.get();
                    Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                    if (items.contains(item)) return SINGLE_SUCCESS;

                    items.add(item);

                    itemHighlight.items.set(items);
                    itemHighlight.info("Added " + item.toString() + " to highlighted items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("clear")
                .executes(context -> {
                    itemHighlight.items.set(new ArrayList<>());
                    itemHighlight.info("Removed all from highlighted items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("remove")
                .executes(context -> {
                    List<Item> items = itemHighlight.items.get();
                    ItemStack itemStack = getItemStack();

                    if (itemStack == null) {
                        error("Not holding an item.");

                        return SINGLE_SUCCESS;
                    }

                    Item item = itemStack.getItem();

                    if (!items.contains(item)) return SINGLE_SUCCESS;

                    items.remove(item);

                    itemHighlight.items.set(items);
                    itemHighlight.info("Removed " + item.toString() + " from highlighted items.");

                    return SINGLE_SUCCESS;
                })
                .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                    .executes(context -> {
                        List<Item> items = itemHighlight.items.get();
                        Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                        if (!items.contains(item)) return SINGLE_SUCCESS;

                        items.remove(item);

                        itemHighlight.items.set(items);
                        itemHighlight.info("Removed " + item.toString() + " from highlighted items.");

                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private void buildBlock(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("block")
            .executes(context -> {
                List<Item> items = inventoryTweaks.autoDropItems.get();
                ItemStack itemStack = getItemStack();

                if (itemStack == null) {
                    error("Not holding an item.");

                    return SINGLE_SUCCESS;
                }

                Item item = itemStack.getItem();

                if (items.contains(item)) return SINGLE_SUCCESS;

                items.add(item);

                inventoryTweaks.autoDropItems.set(items);
                inventoryTweaks.info("Added " + item.toString() + " to automatically dropped items.");

                return SINGLE_SUCCESS;
            })
            .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                .executes(context -> {
                    List<Item> items = inventoryTweaks.autoDropItems.get();
                    Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                    if (items.contains(item)) return SINGLE_SUCCESS;

                    items.add(item);

                    inventoryTweaks.autoDropItems.set(items);
                    inventoryTweaks.info("Added " + item.toString() + " to automatically dropped items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("clear")
                .executes(context -> {
                    inventoryTweaks.autoDropItems.set(new ArrayList<>());
                    inventoryTweaks.info("Removed all from automatically dropped items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("remove")
                .executes(context -> {
                    List<Item> items = inventoryTweaks.autoDropItems.get();
                    ItemStack itemStack = getItemStack();

                    if (itemStack == null) {
                        error("Not holding an item.");

                        return SINGLE_SUCCESS;
                    }

                    Item item = itemStack.getItem();

                    if (!items.contains(item)) return SINGLE_SUCCESS;

                    items.remove(item);

                    inventoryTweaks.autoDropItems.set(items);
                    inventoryTweaks.info("Removed " + item.toString() + " from automatically dropped items.");

                    return SINGLE_SUCCESS;
                })
                .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                    .executes(context -> {
                        List<Item> items = inventoryTweaks.autoDropItems.get();
                        Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                        if (!items.contains(item)) return SINGLE_SUCCESS;

                        items.remove(item);

                        inventoryTweaks.autoDropItems.set(items);
                        inventoryTweaks.info("Removed " + item.toString() + " from automatically dropped items.");

                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private void buildLock(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("lock")
            .executes(context -> {
                List<Item> items = inventoryTweaks.antiDropItems.get();
                ItemStack itemStack = getItemStack();

                if (itemStack == null) {
                    error("Not holding an item.");

                    return SINGLE_SUCCESS;
                }

                Item item = itemStack.getItem();

                if (items.contains(item)) return SINGLE_SUCCESS;

                items.add(item);

                inventoryTweaks.antiDropItems.set(items);
                inventoryTweaks.info("Added " + item.toString() + " to anti drop items.");

                return SINGLE_SUCCESS;
            })
            .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                .executes(context -> {
                    List<Item> items = inventoryTweaks.antiDropItems.get();
                    Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                    if (items.contains(item)) return SINGLE_SUCCESS;

                    items.add(item);

                    inventoryTweaks.antiDropItems.set(items);
                    inventoryTweaks.info("Added " + item.toString() + " to anti drop items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("clear")
                .executes(context -> {
                    inventoryTweaks.antiDropItems.set(new ArrayList<>());
                    inventoryTweaks.info("Removed all from anti drop items.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(literal("remove")
                .executes(context -> {
                    List<Item> items = inventoryTweaks.antiDropItems.get();
                    ItemStack itemStack = getItemStack();

                    if (itemStack == null) {
                        error("Not holding an item.");

                        return SINGLE_SUCCESS;
                    }

                    Item item = itemStack.getItem();

                    if (!items.contains(item)) return SINGLE_SUCCESS;

                    items.remove(item);

                    inventoryTweaks.antiDropItems.set(items);
                    inventoryTweaks.info("Removed " + item.toString() + " from anti drop items.");

                    return SINGLE_SUCCESS;
                })
                .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                    .executes(context -> {
                        List<Item> items = inventoryTweaks.antiDropItems.get();
                        Item item = context.getArgument("item", ItemStackArgument.class).getItem();

                        if (!items.contains(item)) return SINGLE_SUCCESS;

                        items.remove(item);

                        inventoryTweaks.antiDropItems.set(items);
                        inventoryTweaks.info("Removed " + item.toString() + " from anti drop items.");

                        return SINGLE_SUCCESS;
                    })
                )
            )
        );
    }

    private ItemStack getItemStack() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack == null) itemStack = mc.player.getOffHandStack();
        return itemStack.isEmpty() ? null : itemStack;
    }
}
