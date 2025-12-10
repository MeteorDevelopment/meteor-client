/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public class ChestSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Chestplate> chestplate = sgGeneral.add(new EnumSetting.Builder<Chestplate>()
            .name("chestplate")
            .description("Which type of chestplate to swap to.")
            .defaultValue(Chestplate.PreferNetherite)
            .build());

    private final Setting<Boolean> preferEnchanted = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-enchanted")
            .description("Prefers enchanted equipment when swapping")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> discourageCurseOfBinding = sgGeneral.add(new BoolSetting.Builder()
            .name("discourage-curse-of-binding")
            .description("Prefers equipment without curse of binding")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> blockCurseOfBinding = sgGeneral.add(new BoolSetting.Builder()
            .name("block-curse-of-binding")
            .description("Prevents equipment with curse of binding from being equiped")
            .defaultValue(false)
            .visible(discourageCurseOfBinding::get)
            .build());

    private final Setting<Boolean> stayOn = sgGeneral.add(new BoolSetting.Builder()
            .name("stay-on")
            .description("Stays on and activates when you turn it off.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> closeInventory = sgGeneral.add(new BoolSetting.Builder()
            .name("close-inventory")
            .description("Sends inventory close after swap.")
            .defaultValue(true)
            .build());

    public ChestSwap() {
        super(Categories.Player, "chest-swap", "Automatically swaps between a chestplate and an elytra.");
    }

    @Override
    public void onActivate() {
        swap();
        if (!stayOn.get())
            toggle();
    }

    @Override
    public void onDeactivate() {
        if (stayOn.get())
            swap();
    }

    public void swap() {
        ItemStack currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (currentItem.contains(DataComponentTypes.GLIDER)) {
            equipChestplate();
        } else if (currentItem.contains(DataComponentTypes.EQUIPPABLE) && currentItem.get(DataComponentTypes.EQUIPPABLE)
                .slot().getEntitySlotId() == EquipmentSlot.CHEST.getEntitySlotId()) {
            equipElytra();
        } else {
            if (!equipChestplate())
                equipElytra();
        }
    }

    private boolean equipChestplate() {
        assert mc.player != null;
        int bestSlot = -1;
        int bestScore = -1;
        boolean foundPreferred = false;

        for (int i = 0; i < mc.player.getInventory().getMainStacks().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getMainStacks().get(i);
            Item item = itemStack.getItem();

            if (!(item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE)) {
                continue;
            }

            boolean hasBinding = hasCurseOfBinding(itemStack);

            if (hasBinding && discourageCurseOfBinding.get() && blockCurseOfBinding.get()) {
                continue;
            }

            // TODO: add discourage binding logic here
            if (!preferEnchanted.get()) {
                switch (chestplate.get()) {
                    case Diamond:
                        if (item == Items.DIAMOND_CHESTPLATE) {
                            equip(i);
                            return true;
                        }
                        break;
                    case Netherite:
                        if (item == Items.NETHERITE_CHESTPLATE) {
                            equip(i);
                            return true;
                        }
                        break;
                    case PreferDiamond:
                        if (item == Items.DIAMOND_CHESTPLATE) {
                            equip(i);
                            return true;
                        } else if (bestSlot == -1) {
                            bestSlot = i;
                        }
                        break;
                    case PreferNetherite:
                        if (item == Items.NETHERITE_CHESTPLATE) {
                            equip(i);
                            return true;
                        } else if (bestSlot == -1) {
                            bestSlot = i;
                        }
                        break;
                }
                continue;
            }

            boolean isPreferred = (chestplate.get() == Chestplate.Diamond && item == Items.DIAMOND_CHESTPLATE) ||
                    (chestplate.get() == Chestplate.Netherite && item == Items.NETHERITE_CHESTPLATE) ||
                    (chestplate.get() == Chestplate.PreferDiamond && item == Items.DIAMOND_CHESTPLATE) ||
                    (chestplate.get() == Chestplate.PreferNetherite && item == Items.NETHERITE_CHESTPLATE);

            int score = rateArmorEnchantments(itemStack);

            if (hasBinding && discourageCurseOfBinding.get()) {
                isPreferred = false;
                score = 0;
            }

            if (isPreferred) {
                // If there is a preferred chestplate ignore all not preferred chesplates
                if (!foundPreferred || score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                    foundPreferred = true;
                }
            } else if (!foundPreferred) {
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            equip(bestSlot);
        }
        return bestSlot != -1;
    }

    private void equipElytra() {
        assert mc.player != null;
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < mc.player.getInventory().getMainStacks().size(); i++) {
            ItemStack item = mc.player.getInventory().getMainStacks().get(i);

            if (!item.contains(DataComponentTypes.GLIDER)) {
                continue;
            }

            boolean hasBinding = hasCurseOfBinding(item);

            if (hasBinding && discourageCurseOfBinding.get() && blockCurseOfBinding.get()) {
                continue;
            }

            if (!preferEnchanted.get()) {
                bestSlot = i;
                break;
            }

            int score = rateElytraEnchantments(item);

            if (hasBinding && discourageCurseOfBinding.get()) {
                score = 0;
            }

            if (score >= bestScore) {
                bestSlot = i;
                bestScore = score;
            }
        }

        if (bestSlot != -1) {
            equip(bestSlot);
        }
    }

    // 2 points for mending, 1 points for each level of unbreaking and any
    // protection enchant
    private int rateArmorEnchantments(ItemStack item) {
        int score = 0;

        if (getEnchantmentLevel(item, Enchantments.MENDING) > 0) {
            score += 2;
        }

        score += getEnchantmentLevel(item, Enchantments.UNBREAKING);

        int prot = getEnchantmentLevel(item, Enchantments.PROTECTION);

        if (prot == 0) {
            prot = getEnchantmentLevel(item, Enchantments.BLAST_PROTECTION);
        }

        if (prot == 0) {
            prot = getEnchantmentLevel(item, Enchantments.FIRE_PROTECTION);
        }

        if (prot == 0) {
            prot = getEnchantmentLevel(item, Enchantments.PROJECTILE_PROTECTION);
        }

        score += prot;

        return score;
    }

    // 2 points for mending, 1 point for each level of unbreaking
    private int rateElytraEnchantments(ItemStack stack) {
        int score = 0;

        if (getEnchantmentLevel(stack, Enchantments.MENDING) > 0) {
            score += 2;
        }

        score += getEnchantmentLevel(stack, Enchantments.UNBREAKING);

        return score;
    }

    private boolean hasCurseOfBinding(ItemStack stack) {
        if (getEnchantmentLevel(stack, Enchantments.BINDING_CURSE) > 0) {
            return true;
        }

        return false;
    }

    public static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        for (RegistryEntry<Enchantment> enchantments : stack.getEnchantments().getEnchantments()) {
            if (enchantments.toString().contains(enchantment.getValue().toString())) {
                return stack.getEnchantments().getLevel(enchantments);
            }
        }
        return 0;
    }

    private void equip(int slot) {
        InvUtils.move().from(slot).toArmor(2);
        if (closeInventory.get()) {
            // Notchian clients send a Close Window packet with Window ID 0 to close their
            // inventory even though there is never an Open Screen packet for the inventory.
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    @Override
    public void sendToggledMsg() {
        if (stayOn.get())
            super.sendToggledMsg();
        else if (Config.get().chatFeedback.get() && chatFeedback)
            info("Triggered (highlight)%s(default).", title);
    }

    public enum Chestplate {
        Diamond,
        Netherite,
        PreferDiamond,
        PreferNetherite
    }
}
