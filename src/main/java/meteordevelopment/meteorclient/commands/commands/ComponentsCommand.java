/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.DataResult;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ComponentMapArgumentType;
import meteordevelopment.meteorclient.commands.arguments.EntityArgumentType;
import meteordevelopment.meteorclient.commands.arguments.ItemSlotArgumentType;
import meteordevelopment.meteorclient.utils.commands.ArgumentFunction;
import meteordevelopment.meteorclient.utils.commands.ComponentMapReader;
import meteordevelopment.meteorclient.utils.commands.ComponentMapWriter;
import meteordevelopment.meteorclient.utils.commands.CreativeCommandHelper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.component.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.Locale;
import java.util.Set;

public class ComponentsCommand extends Command {
    private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
        error -> Text.stringifiedTranslatable("arguments.item.malformed", error)
    );

    public ComponentsCommand() {
        super("components", "View and modify data components for an item, example: .components add [minecraft:item_name='{\"color\":\"red\",\"text\":\"Red Name\"}']");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("reset")
            .executes(context -> executeReset(mc.player.getInventory().selectedSlot))
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).executes(context ->
                executeReset(ItemSlotArgumentType.getItemSlot(context))
            ))
        );

        builder.then(literal("add")
            .then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(context ->
                executeAdd(context, mc.player.getInventory().selectedSlot)
            ))
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(context ->
                executeAdd(context, ItemSlotArgumentType.getItemSlot(context))
            )))
        );

        builder.then(literal("set")
            .then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(context ->
                executeSet(context, mc.player.getInventory().selectedSlot)
            ))
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(context ->
                executeSet(context, ItemSlotArgumentType.getItemSlot(context))
            )))
        );

        builder.then(literal("remove")
            .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                .executes(context -> executeRemove(context, mc.player.getInventory().selectedSlot))
                .suggests(getComponentSuggestionProvider(context -> mc.player, context -> mc.player.getInventory().selectedSlot))
            )
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                .executes(context -> executeRemove(context, ItemSlotArgumentType.getItemSlot(context)))
                .suggests(getComponentSuggestionProvider(context -> mc.player, ItemSlotArgumentType::getItemSlot))
            ))
        );

        builder.then(literal("get")
            .executes(context -> executeGet(mc.player, mc.player.getInventory().selectedSlot))
            .then(literal("full").executes(context -> executeGetFull(mc.player, mc.player.getInventory().selectedSlot)))
            .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                .executes(context -> executeGet(context, mc.player, mc.player.getInventory().selectedSlot))
                .suggests(getComponentSuggestionProvider(context -> mc.player, context -> mc.player.getInventory().selectedSlot))
            )
            .then(argument("slot", ItemSlotArgumentType.selfSlot())
                .executes(context -> executeGet(mc.player, ItemSlotArgumentType.getItemSlot(context)))
                .then(literal("full").executes(context -> executeGetFull(mc.player, ItemSlotArgumentType.getItemSlot(context))))
                .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                    .executes(context -> executeGet(context, mc.player, ItemSlotArgumentType.getItemSlot(context)))
                    .suggests(getComponentSuggestionProvider(context -> mc.player, ItemSlotArgumentType::getItemSlot))
                )
            )
            .then(argument("entity", EntityArgumentType.entity())
                .executes(context -> executeGet(
                    EntityArgumentType.getEntity(context, "entity"),
                    ItemSlotArgumentType.MAINHAND_SLOT_INDEX
                ))
                .then(literal("full").executes(context -> executeGetFull(
                    EntityArgumentType.getEntity(context, "entity"),
                    ItemSlotArgumentType.MAINHAND_SLOT_INDEX
                )))
                .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                    .executes(context -> executeGet(context, EntityArgumentType.getEntity(context, "entity"), ItemSlotArgumentType.MAINHAND_SLOT_INDEX))
                    .suggests(getComponentSuggestionProvider(context -> EntityArgumentType.getEntity(context, "entity"), context -> ItemSlotArgumentType.MAINHAND_SLOT_INDEX))
                )
                .then(argument("slot", ItemSlotArgumentType.itemSlot())
                    .executes(context -> executeGet(
                        EntityArgumentType.getEntity(context, "entity"),
                        ItemSlotArgumentType.getItemSlot(context)
                    ))
                    .then(literal("full").executes(context -> executeGetFull(
                        EntityArgumentType.getEntity(context, "entity"),
                        ItemSlotArgumentType.getItemSlot(context)
                    )))
                    .then(argument("component", RegistryKeyArgumentType.registryKey(RegistryKeys.DATA_COMPONENT_TYPE))
                        .executes(context -> executeGet(context, EntityArgumentType.getEntity(context, "entity"), ItemSlotArgumentType.getItemSlot(context)))
                        .suggests(getComponentSuggestionProvider(context -> EntityArgumentType.getEntity(context, "entity"), ItemSlotArgumentType::getItemSlot))
                    )
                )
            )
        );

        builder.then(literal("copy")
            .executes(context -> executeCopy(mc.player, mc.player.getInventory().selectedSlot))
            .then(literal("full").executes(context -> executeCopyFull(mc.player, mc.player.getInventory().selectedSlot)))
            .then(argument("slot", ItemSlotArgumentType.selfSlot())
                .executes(context -> executeCopy(mc.player, ItemSlotArgumentType.getItemSlot(context)))
                .then(literal("full").executes(context -> executeCopyFull(mc.player, ItemSlotArgumentType.getItemSlot(context))))
            )
            .then(argument("entity", EntityArgumentType.entity())
                .executes(context -> executeCopy(
                    EntityArgumentType.getEntity(context, "entity"),
                    ItemSlotArgumentType.MAINHAND_SLOT_INDEX
                ))
                .then(literal("full").executes(context -> executeCopyFull(
                    EntityArgumentType.getEntity(context, "entity"),
                    ItemSlotArgumentType.MAINHAND_SLOT_INDEX
                )))
                .then(argument("slot", ItemSlotArgumentType.itemSlot())
                    .executes(context -> executeCopy(
                        EntityArgumentType.getEntity(context, "entity"),
                        ItemSlotArgumentType.getItemSlot(context)
                    ))
                    .then(literal("full").executes(context -> executeCopyFull(
                        EntityArgumentType.getEntity(context, "entity"),
                        ItemSlotArgumentType.getItemSlot(context)
                    )))
                )
            )
        );

        builder.then(literal("paste")
            .executes(context -> executePaste(mc.player.getInventory().selectedSlot))
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).executes(context ->
                executePaste(ItemSlotArgumentType.getItemSlot(context))
            ))
        );

        builder.then(literal("count").then(argument("count", IntegerArgumentType.integer(1, 99))
            .executes(context -> executeCount(
                mc.player.getInventory().selectedSlot,
                IntegerArgumentType.getInteger(context, "count")
            ))
            .then(argument("slot", ItemSlotArgumentType.modifiableSlot()).executes(context -> executeCount(
                ItemSlotArgumentType.getItemSlot(context),
                IntegerArgumentType.getInteger(context, "count")
            )))
        ));

        builder.then(literal("rename").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            ItemStack stack = mc.player.getMainHandStack();
            CreativeCommandHelper.assertValid(stack);

            String name = StringArgumentType.getString(context, "name");
            stack.set(DataComponentTypes.ITEM_NAME, Text.literal(name));
            CreativeCommandHelper.setStack(stack);

            return SINGLE_SUCCESS;
        })));
    }

    private int executeReset(int slot) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        stack.clearComponentChanges();

        CreativeCommandHelper.setStack(stack, slot);
        info("Reset components.");

        return SINGLE_SUCCESS;
    }

    private <S> int executeAdd(CommandContext<S> context, int slot) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        ComponentMap itemComponents = stack.getComponents();
        ComponentMap newComponents = ComponentMapArgumentType.getComponentMap(context, "component");

        ComponentMap testComponents = ComponentMap.of(itemComponents, newComponents);
        DataResult<Unit> dataResult = ItemStack.validateComponents(testComponents);
        dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);

        stack.applyComponentsFrom(testComponents);

        CreativeCommandHelper.setStack(stack, slot);
        info("Added components.");

        return SINGLE_SUCCESS;
    }

    private <S> int executeSet(CommandContext<S> context, int slot) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        ComponentMap components = ComponentMapArgumentType.getComponentMap(context, "component");
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

        CreativeCommandHelper.setStack(stack, slot);
        info("Set components.");

        return SINGLE_SUCCESS;
    }

    private <S> int executeRemove(CommandContext<S> context, int slot) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        @SuppressWarnings("unchecked")
        RegistryKey<ComponentType<?>> componentTypeKey = (RegistryKey<ComponentType<?>>) context.getArgument("component", RegistryKey.class);

        ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(componentTypeKey);
        stack.remove(componentType);

        CreativeCommandHelper.setStack(stack, slot);
        info("Removed (highlight)%s(default).", componentType);

        return SINGLE_SUCCESS;
    }

    private <S> SuggestionProvider<S> getComponentSuggestionProvider(ArgumentFunction<S, Entity> entityArgumentFunction, ArgumentFunction<S, Integer> slotArgumentFunction) {
        return (context, suggestionsBuilder) -> {
            ItemStack stack = entityArgumentFunction.apply(context).getStackReference(slotArgumentFunction.apply(context)).get();
            CreativeCommandHelper.assertValid(stack);

            ComponentMap components = stack.getComponents();
            String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

            CommandSource.forEachMatching(components.getTypes().stream().map(Registries.DATA_COMPONENT_TYPE::getEntry).filter(entry -> entry.getKey().isPresent()).toList(), remaining, entry ->
                entry.getKey().orElseThrow().getValue(),
                entry -> {
                    ComponentType<?> dataComponentType = entry.value();
                    if (dataComponentType.getCodec() != null) suggestionsBuilder.suggest(entry.getKey().orElseThrow().getValue().toString());
                }
            );

            return suggestionsBuilder.buildFuture();
        };
    }

    private int executeGet(Entity entity, int slot) throws CommandSyntaxException {
        ItemStack stack = entity.getStackReference(slot).get();
        CreativeCommandHelper.assertValid(stack);

        SerializedComponents serialized = SerializedComponents.serialize(stack.getComponentChanges());
        info(serialized.formatted());

        return SINGLE_SUCCESS;
    }

    private int executeGetFull(Entity entity, int slot) throws CommandSyntaxException {
        ItemStack stack = entity.getStackReference(slot).get();
        CreativeCommandHelper.assertValid(stack);

        SerializedComponents serialized = SerializedComponents.serialize(stack.getComponents());
        info(serialized.formatted());

        return SINGLE_SUCCESS;
    }

    private <S> int executeGet(CommandContext<S> context, Entity entity, int slot) throws CommandSyntaxException {
        ItemStack stack = entity.getStackReference(slot).get();
        CreativeCommandHelper.assertValid(stack);

        @SuppressWarnings("unchecked")
        RegistryKey<ComponentType<?>> componentTypeKey = (RegistryKey<ComponentType<?>>) context.getArgument("component", RegistryKey.class);

        ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(componentTypeKey);

        if (stack.contains(componentType)) {
            SerializedComponents serialized = SerializedComponents.serialize(stack, componentType);
            info(serialized.formatted());
        } else {
            info(NbtCommand.createCopyButton("[]").append(" []"));
        }

        return SINGLE_SUCCESS;
    }

    private int executeCopy(Entity entity, int slot) throws CommandSyntaxException {
        ItemStack stack = entity.getStackReference(slot).get();
        CreativeCommandHelper.assertValid(stack);

        SerializedComponents serialized = SerializedComponents.serialize(stack.getComponentChanges());

        mc.keyboard.setClipboard(serialized.stringified());
        info(serialized.formatted().append(" data copied!"));

        return SINGLE_SUCCESS;
    }

    private int executeCopyFull(Entity entity, int slot) throws CommandSyntaxException {
        ItemStack stack = entity.getStackReference(slot).get();
        CreativeCommandHelper.assertValid(stack);

        SerializedComponents serialized = SerializedComponents.serialize(stack.getComponents());

        mc.keyboard.setClipboard(serialized.stringified());
        info(serialized.formatted().append(" data copied!"));

        return SINGLE_SUCCESS;
    }

    private int executePaste(int slot) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        stack.applyComponentsFrom(new ComponentMapReader(REGISTRY_ACCESS).consume(new StringReader(mc.keyboard.getClipboard())));
        CreativeCommandHelper.setStack(stack, slot);

        return SINGLE_SUCCESS;
    }

    private int executeCount(int slot, int count) throws CommandSyntaxException {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        CreativeCommandHelper.assertValid(stack);

        stack.setCount(count);
        CreativeCommandHelper.setStack(stack, slot);

        return SINGLE_SUCCESS;
    }

    private record SerializedComponents(String stringified, MutableText formatted) {
        public static SerializedComponents serialize(ComponentMap componentMap) {
            ComponentMapWriter writer = new ComponentMapWriter(REGISTRY_ACCESS);
            String stringified = writer.write(componentMap).resultOrPartial().orElseThrow();
            return new SerializedComponents(stringified, NbtCommand.createCopyButton(stringified).append(ScreenTexts.SPACE).append(writer.writePrettyPrinted(componentMap).resultOrPartial().orElseThrow()));
        }

        public static SerializedComponents serialize(ComponentChanges componentChanges) {
            ComponentMapWriter writer = new ComponentMapWriter(REGISTRY_ACCESS);
            String stringified = writer.write(componentChanges).resultOrPartial().orElseThrow();
            return new SerializedComponents(stringified, NbtCommand.createCopyButton(stringified).append(ScreenTexts.SPACE).append(writer.writePrettyPrinted(componentChanges).resultOrPartial().orElseThrow()));
        }

        public static <T> SerializedComponents serialize(ItemStack stack, ComponentType<T> componentType) {
            T componentValue = stack.get(componentType);
            ComponentMapWriter writer = new ComponentMapWriter(REGISTRY_ACCESS);
            String stringified = writer.write(componentType, componentValue).resultOrPartial().orElseThrow();
            return new SerializedComponents(stringified, NbtCommand.createCopyButton(stringified).append(ScreenTexts.SPACE).append(writer.writePrettyPrinted(componentType, componentValue).resultOrPartial().orElseThrow()));
        }
    }
}
