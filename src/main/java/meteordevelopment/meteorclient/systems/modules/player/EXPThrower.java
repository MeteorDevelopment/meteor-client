/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class EXPThrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("When the module should throw exp")
        .defaultValue(Mode.Manual)
        .build()
    );

    // Manual mode

    private final Setting<Keybind> manualBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("The bind to press for exp to be thrown")
        .visible(() -> mode.get() == Mode.Manual)
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_GRAVE_ACCENT))
        .build()
    );

    // General

    private final Setting<Boolean> replenish = sgGeneral.add(new BoolSetting.Builder()
        .name("replenish")
        .description("Automatically replenishes exp into a selected hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
        .name("exp-slot")
        .description("The slot to replenish exp into.")
        .visible(replenish::get)
        .defaultValue(6)
        .range(1, 9)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("The minimum durability percentage for an item to be repaired.")
        .defaultValue(30)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> armor = sgGeneral.add(new BoolSetting.Builder()
        .name("armor")
        .description("Repairs all repairable armor that you are wearing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hands = sgGeneral.add(new BoolSetting.Builder()
        .name("hands")
        .description("Repairs all repairable items in your hands.")
        .defaultValue(true)
        .build()
    );

    public EXPThrower() {
        super(Categories.Player, "exp-thrower", "Automatically throws XP bottles in your hotbar.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Manual && !manualBind.get().isPressed()) return;

        boolean shouldThrow = false;

        if (armor.get()) {
            for (ItemStack itemStack : mc.player.getInventory().armor) {
                if (needsRepair(itemStack)) {
                    shouldThrow = true;
                    break;
                }
            }
        }

        if (hands.get() && !shouldThrow) {
            for (Hand hand : Hand.values()) {
                if (needsRepair(mc.player.getStackInHand(hand))) {
                    shouldThrow = true;
                    break;
                }
            }
        }

        if (!shouldThrow) return;

        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);

        if (exp.found()) {
            if (!exp.isHotbar() && !exp.isOffhand()) {
                if (!replenish.get()) return;
                InvUtils.move().from(exp.getSlot()).toHotbar(slot.get() - 1);
            }

            Rotations.rotate(-90, mc.player.getYaw(), () -> {
                if (exp.getHand() != null) {
                    mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
                }
                else {
                    InvUtils.swap(exp.getSlot(), true);
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                }
            });
        }
    }

    private boolean needsRepair(ItemStack itemStack) {
        if (itemStack.isEmpty() || EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) < 1) return false;
        return (itemStack.getMaxDamage() - itemStack.getDamage()) / (double) itemStack.getMaxDamage() * 100 <= threshold.get();
    }

    public enum Mode {
        Automatic,
        Manual
    }
}
