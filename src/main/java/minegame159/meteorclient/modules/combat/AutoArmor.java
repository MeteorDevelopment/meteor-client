package minegame159.meteorclient.modules.combat;

//Rewritten by squidoodly 25/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.container.SlotActionType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoArmor extends ToggleModule {
    public enum Prot{
        Protection(Enchantments.PROTECTION),
        Blast_Protection(Enchantments.BLAST_PROTECTION),
        Fire_Protection(Enchantments.FIRE_PROTECTION),
        Projectile_Protection(Enchantments.PROJECTILE_PROTECTION);

        private final Enchantment enchantment;

        Prot(Enchantment enchantment) {
            this.enchantment = enchantment;
        }
    }

    public AutoArmor(){super(Category.Combat, "auto-armor", "Manages your armor for you.");}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Prot> mode = sgGeneral.add(new EnumSetting.Builder<Prot>()
            .name("prioritize")
            .description("Which protection to prioritize.")
            .defaultValue(Prot.Protection)
            .build()
    );

    private final Setting<Boolean> bProtLegs = sgGeneral.add(new BoolSetting.Builder()
            .name("blast-prot-leggings")
            .description("Prioritizes blast protection on leggings")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> preferMending = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Prefers to equip mending than non mending.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> weight = sgGeneral.add(new IntSetting.Builder().name("weight").description("How preferred mending is.")
            .defaultValue(2)
            .min(1)
            .max(10)
            .sliderMax(4)
            .build()
    );

    private final Setting<List<Enchantment>> avoidEnch = sgGeneral.add(new EnchListSetting.Builder()
            .name("avoided-enchantments")
            .description("Enchantments that will only be equipped as a last resort.")
            .defaultValue(setDefaultValue())
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Tries to stop your armor getting broken.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
            .name("break-durability")
            .description("The durability damaged armor is swapped.")
            .defaultValue(10)
            .max(50)
            .min(2)
            .sliderMax(20)
            .build()
    );

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        Prot preMode = mode.get();
        int currentItemScore = 0;
        ItemStack itemStack;
        for (int a = 0; a < 4; a++) {
            itemStack = mc.player.inventory.getArmorStack(a);
            if (EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, itemStack) > 0) continue;
            if (itemStack.getItem() instanceof ArmorItem) {
                if (a == 1 && bProtLegs.get()) {
                    mode.set(Prot.Blast_Protection);
                }
                currentItemScore = getItemScore(itemStack);
            }
            int bestSlot = -1;
            int bestScore = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getInvStack(i);
                if (stack.getItem() instanceof ArmorItem
                        && (((ArmorItem)stack.getItem()).getSlotType().getEntitySlotId() == a)) {
                    int temp = getItemScore(stack);
                    if (bestScore < temp) {
                        bestScore = temp;
                        bestSlot = i;
                    }
                }
            }
            if (bestScore > currentItemScore && bestSlot > -1) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(bestSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(8 - a, 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(bestSlot), 0, SlotActionType.PICKUP);
            }
            mode.set(preMode);
        }
    });

    private int getItemScore(ItemStack itemStack){
        int score = 0;
        if (antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) <= breakDurability.get()) return -1;
        Iterator<Enchantment> bad = avoidEnch.get().iterator();
        for (Enchantment ench = bad.next(); bad.hasNext(); ench = bad.next()) {
            if (EnchantmentHelper.getLevel(ench, itemStack) > 0) return 0;
        }
        score += 8 * EnchantmentHelper.getLevel(mode.get().enchantment, itemStack);
        score += 2 * EnchantmentHelper.getLevel(Enchantments.PROTECTION, itemStack);
        score += 2 * EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, itemStack);
        score += 2 * EnchantmentHelper.getLevel(Enchantments.FIRE_PROTECTION, itemStack);
        score += 2 * EnchantmentHelper.getLevel(Enchantments.PROJECTILE_PROTECTION, itemStack);
        score += 2 * ((ArmorItem) itemStack.getItem()).getProtection();
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        if (preferMending.get() && EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) > 0) score += weight.get();
        return score;
    }

    private List<Enchantment> setDefaultValue() {
        List<Enchantment> enchs = new ArrayList<>();
        enchs.add(Enchantments.BINDING_CURSE);
        enchs.add(Enchantments.FROST_WALKER);
        return enchs;
    }
}
