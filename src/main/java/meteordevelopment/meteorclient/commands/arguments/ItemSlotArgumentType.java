/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.utils.player.EChestMemory;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemSlotArgumentType implements ArgumentType<Integer> {
    private static final ItemSlotArgumentType ONLY_MODIFIABLE = new ItemSlotArgumentType(Mode.ONLY_MODIFIABLE);
    private static final ItemSlotArgumentType SELF_SLOTS = new ItemSlotArgumentType(Mode.SELF_SLOTS);
    private static final ItemSlotArgumentType ALL_SLOTS = new ItemSlotArgumentType(Mode.ALL_SLOTS);
    private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "weapon");
    private static final DynamicCommandExceptionType UNKNOWN_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> Text.translatable("slot.unknown", name));
    public static final int MAINHAND_SLOT_INDEX = EquipmentSlot.MAINHAND.getOffsetEntitySlotId(98);
    public static final int OFFHAND_SLOT_INDEX = EquipmentSlot.OFFHAND.getOffsetEntitySlotId(98);

    private final Mode mode;

    private ItemSlotArgumentType(Mode mode) {
        this.mode = mode;
    }

    public static ItemSlotArgumentType modifiableSlot() {
        return ONLY_MODIFIABLE;
    }

    public static ItemSlotArgumentType selfSlot() {
        return SELF_SLOTS;
    }

    public static ItemSlotArgumentType itemSlot() {
        return ALL_SLOTS;
    }

    public static <S> int getItemSlot(CommandContext<S> context) {
        return context.getArgument("slot", int.class);
    }

    public static <S> int getItemSlot(CommandContext<S> context, String name) {
        return context.getArgument(name, int.class);
    }

    @Override
    public Integer parse(StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();

        Set<String> allowedValues = this.mode.contextAwareAllowedValues.apply(mc.player);

        if (allowedValues.contains(string)) {
            @Nullable SlotRange range = SlotRanges.fromName(string);
            if (range != null) {
                return range.getSlotIds().getInt(0);
            }
        }

        throw UNKNOWN_SLOT_EXCEPTION.create(string);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(this.mode.contextAwareAllowedValues.apply(mc.player), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private enum Mode {
        ONLY_MODIFIABLE(new ObjectOpenHashSet<>(Util.make(new ArrayList<>(), list -> {
            for (int i = 0; i < 9; i++) list.add("hotbar." + i);
            for (int i = 0; i < 27; i++) list.add("inventory." + i);
            list.add("weapon");
            list.add("weapon.mainhand");
            list.add("weapon.offhand");
            list.add("armor.head");
            list.add("armor.chest");
            list.add("armor.legs");
            list.add("armor.feet");
            list.add("armor.body");
            list.add("player.cursor");
            for (int i = 0; i < 4; i++) list.add("player.crafting." + i);
        }))),
        SELF_SLOTS(
            Util.make(new ObjectOpenHashSet<>(ONLY_MODIFIABLE.allowedValues), set -> {
                for (int i = 0; i < 27; i++) {
                    set.add("enderchest." + i);
                }
            }),
            e -> Util.make(new ObjectOpenHashSet<>(ONLY_MODIFIABLE.allowedValues), set -> {
                if (EChestMemory.isKnown()) {
                    for (int i = 0; i < 27; i++) {
                        set.add("enderchest." + i);
                    }
                }
            })
        ),
        ALL_SLOTS(
            SlotRanges.streamSingleSlotNames().collect(Collectors.toSet())
        );

        private final Set<String> allowedValues;
        private final Function<Entity, Set<String>> contextAwareAllowedValues;

        Mode(Set<String> allowedValues) {
            this(allowedValues, e -> allowedValues);
        }

        Mode(Set<String> allowedValues, Function<Entity, Set<String>> contextAwareAllowedValues) {
            this.allowedValues = allowedValues;
            this.contextAwareAllowedValues = contextAwareAllowedValues;
        }
    }
}
