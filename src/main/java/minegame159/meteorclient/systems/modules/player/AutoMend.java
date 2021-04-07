/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

//Updated by squidoodly 18/06/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.combat.AutoArmor;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;

public class AutoMend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> swords = sgGeneral.add(new BoolSetting.Builder()
            .name("swords")
            .description("Moves swords.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> armourSlots = sgGeneral.add(new BoolSetting.Builder()
            .name("use-armor-slots")
            .description("Whether or not to use armor slots to mend items quicker.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> removeFinished = sgGeneral.add(new BoolSetting.Builder()
            .name("remove-when-finished")
            .description("The items will be moved out of active slots if there are no items to replace, but space in your inventory.")
            .defaultValue(true)
            .build()
    );

    public AutoMend() {
        super(Categories.Player, "auto-mend", "Automatically replaces equipped items and items in your offhand with Mending when fully repaired.");
    }

    private void replaceItem(boolean offhandEmpty) {
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) == 0 || !itemStack.isDamaged()) continue;
            if (!swords.get() && itemStack.getItem() instanceof SwordItem) continue;

            InvUtils.move().from(i).toOffhand();

            break;
        }
        if(!mc.player.getOffHandStack().isDamaged() && removeFinished.get() && mc.player.inventory.getEmptySlot() != -1){
            InvUtils.move().fromOffhand().to(mc.player.inventory.getEmptySlot());
        }
    }

    private boolean checkSlot(ItemStack itemStack, int slot){
        boolean correct = false;
        if(slot == 5 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.HEAD) correct = true;
        else if(slot == 6 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.CHEST) correct = true;
        else if(slot == 7 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.LEGS) correct = true;
        else if(slot == 8 && ((ArmorItem) itemStack.getItem()).getSlotType() == EquipmentSlot.FEET) correct = true;
        return correct;
    }

    private void replaceArmour(int slot, boolean empty){
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if(!(itemStack.getItem() instanceof ArmorItem)) continue;
            if(!checkSlot(mc.player.inventory.getStack(i), slot)) continue;
            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) == 0 || !itemStack.isDamaged()) continue;

            InvUtils.move().from(i).toId(slot);

            break;
        }
        if(!mc.player.inventory.getStack(39 - (slot - 5)).isDamaged() && removeFinished.get() && mc.player.inventory.getEmptySlot() != -1){
            InvUtils.move().fromId(slot).to(mc.player.inventory.getEmptySlot());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.currentScreenHandler.getStacks().size() != 46) return;

        if (mc.player.getOffHandStack().isEmpty()) replaceItem(true);
        else if (!mc.player.getOffHandStack().isDamaged()) replaceItem(false);
        else if (EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.getOffHandStack()) == 0) replaceItem(false);

        if(armourSlots.get()) {
            if(Modules.get().isActive(AutoArmor.class)) {
                ChatUtils.moduleWarning(this, "Cannot use armor slots while AutoArmor is active. Please disable AutoArmor and try again. Disabling Use Armor Slots.");
                armourSlots.set(false);
            }
            for (int i = 5; i < 9; i++) {
                if (mc.player.inventory.getStack(39 - (i - 5)).isEmpty()) replaceArmour(i, true);
                else if (!mc.player.inventory.getStack(39 - (i - 5)).isDamaged()) replaceArmour(i, false);
                else if (EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.inventory.getStack(39 - (i - 5))) == 0) replaceArmour(i, false);
            }
        }
    }
}
