/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;

public class ChestSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Chestplate> chestplate = sgGeneral.add(new EnumSetting.Builder<Chestplate>()
        .name("chestplate")
        .description("Which type of chestplate to swap to.")
        .defaultValue(Chestplate.Best)
        .build()
    );

    private final Setting<Boolean> enchants = sgGeneral.add(new BoolSetting.Builder()
        .name("enchantments")
        .description("Whether to handle enchantments when swapping chestplate.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stayOn = sgGeneral.add(new BoolSetting.Builder()
        .name("stay-on")
        .description("Stays on and activates when you turn it off.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> closeInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("close-inventory")
        .description("Sends inventory close after swap.")
        .defaultValue(false)
        .build()
    );

    public ChestSwap() {
        super(Categories.Player, "chest-swap", "Automatically swaps between a chestplate and an elytra.");
    }

    @Override
    public void onActivate() {
        swap();
        if (!stayOn.get()) toggle();
    }

    @Override
    public void onDeactivate() {
        if (stayOn.get()) swap();
    }

    public void swap() {
        Item currentItem = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem();

        if (currentItem == Items.ELYTRA) {
            equipChestplate();
        } else if (currentItem instanceof ArmorItem && ((ArmorItem) currentItem).getSlotType() == EquipmentSlot.CHEST) {
            equipElytra();
        } else {
            if (!equipChestplate()) equipElytra();
        }
    }

    private boolean equipChestplate() {
        int bestSlot = -1;
        int score = 0;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack itemStack = mc.player.getInventory().main.get(i);

            if (chestplate.get().item == itemStack.getItem() || (chestplate.get() == Chestplate.Best && itemStack.getItem() instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.CHEST)) {
                int newScore = getScore(itemStack);

                if (newScore > score) {
                    score = newScore;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) equip(bestSlot);
        return bestSlot != -1;
    }

    // modified AutoArmor getScore
    private int getScore(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;

        int score = 0;
        Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
        Utils.getEnchantments(itemStack, enchantments);
        if (enchants.get()) {
            score += Utils.getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            score += Utils.getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
            score += Utils.getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
            score += Utils.getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
            score += Utils.getEnchantmentLevel(enchantments, Enchantments.UNBREAKING);
            score += 2 * Utils.getEnchantmentLevel(enchantments, Enchantments.MENDING);
        }
        score += itemStack.getItem() instanceof ArmorItem armorItem ? armorItem.getProtection() : 0;
        score += itemStack.getItem() instanceof ArmorItem armorItem ? (int) armorItem.getToughness() : 0;

        return score;
    }

    private void equipElytra() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            Item item = mc.player.getInventory().main.get(i).getItem();

            if (item == Items.ELYTRA) {
                equip(i);
                break;
            }
        }
    }

    private void equip(int slot) {
        InvUtils.move().from(slot).toArmor(2);
        if (closeInventory.get()) {
            // Notchian clients send a Close Window packet with Window ID 0 to close their inventory even though there is never an Open Screen packet for the inventory.
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    @Override
    public void sendToggledMsg() {
        if (stayOn.get()) super.sendToggledMsg();
        else if (Config.get().chatFeedback.get() && chatFeedback) info("Triggered (highlight)%s(default).", title);
    }

    public enum Chestplate {
        Leather(Items.LEATHER_CHESTPLATE),
        Chainmail(Items.CHAINMAIL_CHESTPLATE),
        Gold(Items.GOLDEN_CHESTPLATE),
        Iron(Items.IRON_CHESTPLATE),
        Diamond(Items.DIAMOND_CHESTPLATE),
        Netherite(Items.NETHERITE_CHESTPLATE),
        Best(null);

        final Item item;
        Chestplate(Item item) {
            this.item = item;
        }
    }
}
