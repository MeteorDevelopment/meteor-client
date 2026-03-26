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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NbtCommand extends Command {
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
        error -> Component.translatableEscape("arguments.item.malformed", error)
    );
    private final Component copyButton = Component.literal("NBT").setStyle(Style.EMPTY
        .applyFormat(ChatFormatting.UNDERLINE)
        .withClickEvent(new MeteorClickEvent(
            this.toString("copy")
        ))
        .withHoverEvent(new HoverEvent.ShowText(
            Component.literal("Copy the NBT data to your clipboard.")
        )));

    public NbtCommand() {
        super("nbt", "Modifies NBT data for an item, example: .nbt add {display:{Name:'{\"text\":\"$cRed Name\"}'}}");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("add").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();

            if (validBasic(stack)) {
                DataComponentMap itemComponents = stack.getComponents();
                DataComponentMap newComponents = ComponentMapArgumentType.getComponentMap(ctx, "component");

                DataComponentMap testComponents = DataComponentMap.composite(itemComponents, newComponents);
                ItemStack testStack = stack.copy();
                testStack.applyComponents(testComponents);
                DataResult<ItemStack> dataResult = ItemStack.validateStrict(testStack);
                dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);

                stack.applyComponents(testComponents);

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("set").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();

            if (validBasic(stack)) {
                DataComponentMap components = ComponentMapArgumentType.getComponentMap(ctx, "component");
                PatchedDataComponentMap stackComponents = (PatchedDataComponentMap) stack.getComponents();

                ItemStack testStack = stack.copy();
                testStack.applyComponents(components);
                DataResult<ItemStack> dataResult = ItemStack.validateStrict(testStack);
                dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);

                DataComponentPatch.Builder changesBuilder = DataComponentPatch.builder();
                Set<DataComponentType<?>> types = stackComponents.keySet();

                //set changes
                for (TypedDataComponent<?> entry : components) {
                    changesBuilder.set(entry);
                    types.remove(entry.type());
                }

                //remove the rest
                for (DataComponentType<?> type : types) {
                    changesBuilder.remove(type);
                }

                stackComponents.applyPatch(changesBuilder.build());

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("component", ResourceKeyArgument.key(Registries.DATA_COMPONENT_TYPE)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();

            if (validBasic(stack)) {
                @SuppressWarnings("unchecked")
                ResourceKey<DataComponentType<?>> componentTypeKey = (ResourceKey<DataComponentType<?>>) ctx.getArgument("component", ResourceKey.class);

                DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentTypeKey);

                PatchedDataComponentMap components = (PatchedDataComponentMap) stack.getComponents();
                components.applyPatch(DataComponentPatch.builder().remove(componentType).build());

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        }).suggests((ctx, suggestionsBuilder) -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();
            if (stack != ItemStack.EMPTY) {
                DataComponentMap components = stack.getComponents();
                String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

                SharedSuggestionProvider.filterResources(components.keySet().stream().map(BuiltInRegistries.DATA_COMPONENT_TYPE::wrapAsHolder).toList(), remaining, entry -> {
                    if (entry.unwrapKey().isPresent()) return entry.unwrapKey().get().identifier();
                    return null;
                }, entry -> {
                    DataComponentType<?> dataComponentType = entry.value();
                    if (dataComponentType.codec() != null) {
                        if (entry.unwrapKey().isPresent()) {
                            suggestionsBuilder.suggest(entry.unwrapKey().get().identifier().toString());
                        }
                    }
                });
            }

            return suggestionsBuilder.buildFuture();
        })));

        builder.then(literal("get").executes(context -> {
            DataAccessor dataCommandObject = new EntityDataAccessor(mc.player);
            NbtPathArgument.NbtPath handPath = NbtPathArgument.NbtPath.of("SelectedItem");

            MutableComponent text = Component.empty().append(copyButton);

            try {
                List<Tag> nbtElement = handPath.get(dataCommandObject.getData());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtUtils.toPrettyComponent(nbtElement.getFirst()));
                }
            } catch (CommandSyntaxException e) {
                text.append("{}");
            }

            info(text);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("copy").executes(context -> {
            DataAccessor dataCommandObject = new EntityDataAccessor(mc.player);
            NbtPathArgument.NbtPath handPath = NbtPathArgument.NbtPath.of("SelectedItem");

            MutableComponent text = Component.empty().append(copyButton);
            String nbt = "{}";

            try {
                List<Tag> nbtElement = handPath.get(dataCommandObject.getData());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtUtils.toPrettyComponent(nbtElement.getFirst()));
                    nbt = nbtElement.getFirst().toString();
                }
            } catch (CommandSyntaxException e) {
                text.append("{}");
            }

            mc.keyboardHandler.setClipboard(nbt);

            text.append(" data copied!");
            info(text);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("count").then(argument("count", IntegerArgumentType.integer(-127, 127)).executes(context -> {
            ItemStack stack = mc.player.getInventory().getSelectedItem();

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
        mc.player.connection.send(new ServerboundSetCreativeModeSlotPacket(36 + mc.player.getInventory().getSelectedSlot(), stack));
    }

    private boolean validBasic(ItemStack stack) {
        if (!mc.player.getAbilities().instabuild) {
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
