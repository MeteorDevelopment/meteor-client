/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AutoArmor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Protection> preferredProtection = sgGeneral.add(new EnumSetting.Builder<Protection>()
        .name("preferred-protection")
        .description("Which type of protection to prefer.")
        .defaultValue(Protection.Protection)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("The delay between equipping armor pieces.")
        .defaultValue(1)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<List<Enchantment>> avoidedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("avoided-enchantments")
        .description("Enchantments that should be avoided.")
        .defaultValue(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER)
        .build()
    );

    private final Setting<Boolean> blastLeggings = sgGeneral.add(new BoolSetting.Builder()
        .name("blast-prot-leggings")
        .description("Uses blast protection for leggings regardless of preferred protection.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Takes off armor if it is about to break.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-elytra")
        .description("Will not replace your elytra if you have it equipped.")
        .defaultValue(true)
        .build()
    );

    private final Object2IntMap<Enchantment> enchantments = new Object2IntOpenHashMap<>();
    private final ArmorPiece[] armorPieces = new ArmorPiece[4];
    private final ArmorPiece helmet = new ArmorPiece(3);
    private final ArmorPiece chestplate = new ArmorPiece(2);
    private final ArmorPiece leggings = new ArmorPiece(1);
    private final ArmorPiece boots = new ArmorPiece(0);
    private int timer;

    public AutoArmor() {
        super(Categories.Combat, "auto-armor", "Automatically equips armor.");

        armorPieces[0] = helmet;
        armorPieces[1] = chestplate;
        armorPieces[2] = leggings;
        armorPieces[3] = boots;
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        // Wait for timer (delay)
        if (timer > 0) {
            timer--;
            return;
        }

        // Reset armor pieces
        for (ArmorPiece armorPiece : armorPieces) armorPiece.reset();

        // Loop through items in inventory
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.isEmpty() || !(itemStack.getItem() instanceof ArmorItem)) continue;

            // Check for durability if anti break is enabled
            if (antiBreak.get() && itemStack.isDamageable() && itemStack.getMaxDamage() - itemStack.getDamage() <= 10) {
                continue;
            }

            // Get enchantments on the item
            Utils.getEnchantments(itemStack, enchantments);

            // Check for avoided enchantments
            if (hasAvoidedEnchantment()) continue;

            // Add the item to the correct armor piece
            switch (getItemSlotId(itemStack)) {
                case 0 -> boots.add(itemStack, i);
                case 1 -> leggings.add(itemStack, i);
                case 2 -> chestplate.add(itemStack, i);
                case 3 -> helmet.add(itemStack, i);
            }
        }

        // Apply armor pieces
        for (ArmorPiece armorPiece : armorPieces) armorPiece.calculate();
        Arrays.sort(armorPieces, Comparator.comparingInt(ArmorPiece::getSortScore));
        for (ArmorPiece armorPiece : armorPieces) armorPiece.apply();
    }

    private boolean hasAvoidedEnchantment() {
        for (Enchantment enchantment : avoidedEnchantments.get()) {
            if (enchantments.containsKey(enchantment)) return true;
        }

        return false;
    }

    private int getItemSlotId(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ElytraItem) return 2;
        return ((ArmorItem) itemStack.getItem()).getSlotType().getEntitySlotId();
    }

    private int getScore(ItemStack itemStack) {
        if (itemStack.isEmpty()) return 0;

        // Score calculated based on enchantments, protection and toughness
        int score = 0;

        // Prefer blast protection on leggings if enabled
        Enchantment protection = preferredProtection.get().enchantment;
        if (itemStack.getItem() instanceof ArmorItem && blastLeggings.get() && getItemSlotId(itemStack) == 1) {
            protection = Enchantments.BLAST_PROTECTION;
        }

        score += 3 * enchantments.getInt(protection);
        score += enchantments.getInt(Enchantments.PROTECTION);
        score += enchantments.getInt(Enchantments.BLAST_PROTECTION);
        score += enchantments.getInt(Enchantments.FIRE_PROTECTION);
        score += enchantments.getInt(Enchantments.PROJECTILE_PROTECTION);
        score += enchantments.getInt(Enchantments.UNBREAKING);
        score += 2 * enchantments.getInt(Enchantments.MENDING);
        score += itemStack.getItem() instanceof ArmorItem ? ((ArmorItem) itemStack.getItem()).getProtection() : 0;
        score += itemStack.getItem() instanceof ArmorItem ? ((ArmorItem) itemStack.getItem()).getToughness() : 0;

        return score;
    }

    private boolean cannotSwap() {
        return timer > 0;
    }

    private void swap(int from, int armorSlotId) {
        InvUtils.move().from(from).toArmor(armorSlotId);

        // Apply delay
        timer = delay.get();
    }

    private void moveToEmpty(int armorSlotId) {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                InvUtils.move().fromArmor(armorSlotId).to(i);

                // Apply delay
                timer = delay.get();

                break;
            }
        }
    }

    public enum Protection {
        Protection(Enchantments.PROTECTION),
        BlastProtection(Enchantments.BLAST_PROTECTION),
        FireProtection(Enchantments.FIRE_PROTECTION),
        ProjectileProtection(Enchantments.PROJECTILE_PROTECTION);

        private final Enchantment enchantment;

        Protection(Enchantment enchantment) {
            this.enchantment = enchantment;
        }
    }

    private class ArmorPiece {
        private final int id;

        private int bestSlot;
        private int bestScore;

        private int score;
        private int durability;

        public ArmorPiece(int id) {
            this.id = id;
        }

        public void reset() {
            bestSlot = -1;
            bestScore = -1;
            score = -1;
            durability = Integer.MAX_VALUE;
        }

        public void add(ItemStack itemStack, int slot) {
            // Calculate armor piece score and check if its higher than the last one
            int score = getScore(itemStack);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        public void calculate() {
            if (cannotSwap()) return;

            ItemStack itemStack = mc.player.getInventory().getArmorStack(id);

            // Check if the item is an elytra
            if ((ignoreElytra.get() || Modules.get().isActive(ChestSwap.class)) && itemStack.getItem() == Items.ELYTRA) {
                score = Integer.MAX_VALUE; // Setting score to Integer.MAX_VALUE so its now swapped later
                return;
            }

            Utils.getEnchantments(itemStack, enchantments);

            // Return if current armor piece has Curse of Binding
            if (enchantments.containsKey(Enchantments.BINDING_CURSE)) {
                score = Integer.MAX_VALUE; // Setting score to Integer.MAX_VALUE so its now swapped later
                return;
            }

            // Calculate current score
            score = getScore(itemStack);
            score = decreaseScoreByAvoidedEnchantments(score);
            score = applyAntiBreakScore(score, itemStack);

            // Calculate durability
            if (!itemStack.isEmpty()) {
                durability = itemStack.getMaxDamage() - itemStack.getDamage();
            }
        }

        public int getSortScore() {
            if (antiBreak.get() && durability <= 10) return -1;
            return bestScore;
        }

        public void apply() {
            // Integer.MAX_VALUE check is there because it indicates that the current piece shouldn't be moved
            if (cannotSwap() || score == Integer.MAX_VALUE) return;

            // Check if new score is better and swap if it is
            if (bestScore > score) swap(bestSlot, id);
            else if (antiBreak.get() && durability <= 10) {
                // If no better piece has been found but current piece is broken find an empty slot and move it there
                moveToEmpty(id);
            }
        }

        private int decreaseScoreByAvoidedEnchantments(int score) {
            for (Enchantment enchantment : avoidedEnchantments.get()) {
                score -= 2 * enchantments.getInt(enchantment);
            }

            return score;
        }

        private int applyAntiBreakScore(int score, ItemStack itemStack) {
            if (antiBreak.get() && itemStack.isDamageable() && itemStack.getMaxDamage() - itemStack.getDamage() <= 10) {
                return -1;
            }

            return score;
        }
    }
}
