/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.commands.CreativeCommandHelper;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.Optional;

public class TransmogrifyCommand extends Command {
    public TransmogrifyCommand() {
        super("transmogrify", "Camouflages your held item.", "transmog");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("item").then(argument("item", RegistryKeyArgumentType.registryKey(RegistryKeys.ITEM)).executes(context -> {
            ItemStack stack = mc.player.getMainHandStack();
            CreativeCommandHelper.assertValid(stack);

            Identifier itemId = context.getArgument("item", RegistryKey.class).getValue();
            Item item = Registries.ITEM.get(itemId);

            transmogrify(stack, itemId, item.getName());
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("model").then(argument("model", IdentifierArgumentType.identifier()).executes(context -> {
            ItemStack stack = mc.player.getMainHandStack();
            CreativeCommandHelper.assertValid(stack);

            Identifier modelId = context.getArgument("model", Identifier.class);

            MeteorExecutor.execute(() -> {
                // check if resource exists
                Identifier resourceId = modelId.withPath(path -> "models/item/" + path + ".json");
                Optional<Resource> modelResource = mc.getResourceManager().getResource(resourceId);

                MinecraftClient.getInstance().execute(() -> { // fun fact: ChatUtils#error is not thread safe
                    if (modelResource.isEmpty()) {
                        error("Given model identifier does not exist.");
                    } else {
                        // check if held item changed during resource check
                        if (mc.player.getMainHandStack().getItem() != stack.getItem()) {
                            error("Dont switch held items while transmogrifying >:(");
                        } else {
                            try {
                                transmogrify(stack, modelId, null);
                            } catch (CommandSyntaxException e) {
                                error(e.getMessage());
                            }
                        }
                    }
                });
            });

            return SINGLE_SUCCESS;
        })));
    }

    private void transmogrify(ItemStack stack, Identifier modelId, @Nullable Text name) throws CommandSyntaxException {
        // check whether the old and new item models match
        Identifier oldModel = stack.get(DataComponentTypes.ITEM_MODEL);
        if ((oldModel != null && oldModel.equals(modelId)) || Registries.ITEM.getId(stack.getItem()).equals(modelId)) {
            info(Text.literal("Nothing changed...").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
            return;
        }

        stack.set(DataComponentTypes.ITEM_MODEL, modelId);
        if (name != null) stack.set(DataComponentTypes.ITEM_NAME, name);

        CreativeCommandHelper.setStack(stack);
        info(Text.literal("Whoosh!").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)));
    }
}
