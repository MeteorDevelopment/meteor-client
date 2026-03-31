/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DataResult;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ComponentMapArgumentType;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.component.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NbtCommand extends Command {
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
        error -> MutableComponent.stringifiedTranslatable("arguments.item.malformed", error)
    );
    private final MutableComponent copyButton = MutableComponent.literal("NBT").setStyle(Style.EMPTY
        .withFormatting(ChatFormatting.UNDERLINE)
        .withClickEvent(new MeteorClickEvent(
            this.toString("copy")
        ))
        .withHoverEvent(new HoverEvent.ShowText(
            MutableComponent.literal("Copy the NBT data to your clipboard.")
        )));

    public NbtCommand() {
        super("nbt", "Modifies NBT data for an item, example: .nbt add {display:{Name:'{\"text\":\"$cRed Name\"}'}}");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("add").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();

            if (validBasic(stack)) {
                ComponentMap itemComponents = stack.getComponents();
                ComponentMap newComponents = ComponentMapArgumentType.getComponentMap(ctx, "component");

                ComponentMap testComponents = ComponentMap.of(itemComponents, newComponents);
                DataResult<Unit> dataResult = ItemStack.validateComponents(testComponents);
                dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);

                stack.applyComponentsFrom(testComponents);

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("set").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();

            if (validBasic(stack)) {
                ComponentMap components = ComponentMapArgumentType.getComponentMap(ctx, "component");
                MergedComponentMap stackComponents = (MergedComponentMap) stack.getComponents();

                DataResult<Unit> dataResult = ItemStack.validateComponents(components);
                dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);

                ComponentChanges.Builder changesBuilder = ComponentChanges.builder();
                Set<ComponentType<?>> types = stackComponents.getTypes();

                //set changes
                for (Component<?> entry : components) {
                    changesBuilder.add(entry);
                    types.remove(entry.type());
                }

                //remove the rest
                for (ComponentType<?> type : types) {
                    changesBuilder.remove(type);
                }

                stackComponents.applyChanges(changesBuilder.build());

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("component", ResourceKeyArgument.registryKey(Registries.DATA_COMPONENT_TYPE)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();

            if (validBasic(stack)) {
                @SuppressWarnings("unchecked")
                ResourceKey<ComponentType<?>> componentTypeKey = (ResourceKey<ComponentType<?>>) ctx.getArgument("component", ResourceKey.class);

                ComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentTypeKey);

                MergedComponentMap components = (MergedComponentMap) stack.getComponents();
                components.applyChanges(ComponentChanges.builder().remove(componentType).build());

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        }).suggests((ctx, suggestionsBuilder) -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();
            if (stack != ItemStack.EMPTY) {
                ComponentMap components = stack.getComponents();
                String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

                SharedSuggestionProvider.forEachMatching(components.getTypes().stream().map(BuiltInRegistries.DATA_COMPONENT_TYPE::getEntry).toList(), remaining, entry -> {
                    if (entry.getKey().isPresent()) return entry.getKey().get().getValue();
                    return null;
                }, entry -> {
                    ComponentType<?> dataComponentType = entry.value();
                    if (dataComponentType.getCodec() != null) {
                        if (entry.getKey().isPresent()) {
                            suggestionsBuilder.suggest(entry.getKey().get().getValue().toString());
                        }
                    }
                });
            }

            return suggestionsBuilder.buildFuture();
        })));

        builder.then(literal("get").executes(context -> {
            DataAccessor dataCommandObject = new EntityDataObject(mc.player);
            NbtPathArgument.NbtPath handPath = NbtPathArgument.NbtPath.parse("SelectedItem");

            MutableComponent text = MutableComponent.empty().append(copyButton);

            try {
                List<Tag> nbtElement = handPath.get(dataCommandObject.getNbt());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtUtils.toPrettyPrintedText(nbtElement.getFirst()));
                }
            } catch (CommandSyntaxException e) {
                text.append("{}");
            }

            info(text);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("copy").executes(context -> {
            DataAccessor dataCommandObject = new EntityDataObject(mc.player);
            NbtPathArgument.NbtPath handPath = NbtPathArgument.NbtPath.parse("SelectedItem");

            MutableComponent text = MutableComponent.empty().append(copyButton);
            String nbt = "{}";

            try {
                List<Tag> nbtElement = handPath.get(dataCommandObject.getNbt());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtUtils.toPrettyPrintedText(nbtElement.getFirst()));
                    nbt = nbtElement.getFirst().toString();
                }
            } catch (CommandSyntaxException e) {
                text.append("{}");
            }

            mc.keyboard.setClipboard(nbt);

            text.append(" data copied!");
            info(text);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("count").then(argument("count", IntegerArgumentType.integer(-127, 127)).executes(context -> {
            ItemStack stack = mc.player.getInventory().getSelectedStack();

            if (validBasic(stack)) {
                int count = IntegerArgumentType.getInteger(context, "count");
                stack.setCount(count);
                setStack(stack);
                info("Set mainhand stack count to %s.", count);
            }

            return SINGLE_SUCCESS;
        })));
    }

    private void setStack(ItemStack stack) {
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), stack));
    }

    private boolean validBasic(ItemStack stack) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode only.");
            return false;
        }

        if (stack == ItemStack.EMPTY) {
            error("You must hold an item in your main hand.");
            return false;
        }
        return true;
    }
}
