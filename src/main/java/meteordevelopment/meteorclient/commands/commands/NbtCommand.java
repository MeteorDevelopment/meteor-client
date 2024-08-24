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
import net.minecraft.command.CommandSource;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.EntityDataObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NbtCommand extends Command {
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
        error -> Text.stringifiedTranslatable("arguments.item.malformed", error)
    );
    private final Text copyButton = Text.literal("NBT").setStyle(Style.EMPTY
        .withFormatting(Formatting.UNDERLINE)
        .withClickEvent(new MeteorClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            this.toString("copy")
        ))
        .withHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Text.literal("Copy the NBT data to your clipboard.")
        )));

    public NbtCommand() {
        super("nbt", "Modifies NBT data for an item, example: .nbt add {display:{Name:'{\"text\":\"$cRed Name\"}'}}");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();

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
            ItemStack stack = mc.player.getInventory().getMainHandStack();

            if (validBasic(stack)) {
                ComponentMap components = ComponentMapArgumentType.getComponentMap(ctx, "component");
                ComponentMapImpl stackComponents = (ComponentMapImpl) stack.getComponents();

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

        builder.then(literal("remove").then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE)).executes(ctx -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();

            if (validBasic(stack)) {
                @SuppressWarnings("unchecked")
                RegistryKey<ComponentType<?>> componentTypeKey = (RegistryKey<ComponentType<?>>) ctx.getArgument("component", RegistryKey.class);

                ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(componentTypeKey);

                ComponentMapImpl components = (ComponentMapImpl) stack.getComponents();
                components.applyChanges(ComponentChanges.builder().remove(componentType).build());

                setStack(stack);
            }

            return SINGLE_SUCCESS;
        }).suggests((ctx, suggestionsBuilder) -> {
            ItemStack stack = mc.player.getInventory().getMainHandStack();
            if (stack != ItemStack.EMPTY) {
                ComponentMap components = stack.getComponents();
                String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

                CommandSource.forEachMatching(components.getTypes().stream().map(Registries.DATA_COMPONENT_TYPE::getEntry).toList(), remaining, entry -> {
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
            DataCommandObject dataCommandObject = new EntityDataObject(mc.player);
            NbtPathArgumentType.NbtPath handPath = NbtPathArgumentType.NbtPath.parse("SelectedItem");

            MutableText text = Text.empty().append(copyButton);

            try {
                List<NbtElement> nbtElement = handPath.get(dataCommandObject.getNbt());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtHelper.toPrettyPrintedText(nbtElement.getFirst()));
                }
            } catch (CommandSyntaxException e) {
                text.append("{}");
            }

            info(text);

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("copy").executes(context -> {
            DataCommandObject dataCommandObject = new EntityDataObject(mc.player);
            NbtPathArgumentType.NbtPath handPath = NbtPathArgumentType.NbtPath.parse("SelectedItem");

            MutableText text = Text.empty().append(copyButton);
            String nbt = "{}";

            try {
                List<NbtElement> nbtElement = handPath.get(dataCommandObject.getNbt());
                if (!nbtElement.isEmpty()) {
                    text.append(" ").append(NbtHelper.toPrettyPrintedText(nbtElement.getFirst()));
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
            ItemStack stack = mc.player.getInventory().getMainHandStack();

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
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().selectedSlot, stack));
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
