/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ItemListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AutoMend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
            .name("blacklist")
            .description("Item blacklist.")
            .defaultValue(new ArrayList<>(0))
            .filter(Item::isDamageable)
            .build()
    );

    private final Setting<Boolean> force = sgGeneral.add(new BoolSetting.Builder()
            .name("force")
            .description("Replaces item in offhand even if there is some other non-repairable item.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disables when there are no more items to repair.")
            .defaultValue(true)
            .build()
    );

    private boolean didMove;

    public AutoMend() {
        super(Categories.Player, "auto-mend", "Automatically replaces items in your offhand with mending when fully repaired.");
    }

    @Override
    public void onActivate() {
        didMove = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (shouldWait()) return;

        int slot = getSlot();

        if (slot == -1) {
            if (autoDisable.get()) {
                ChatUtils.moduleInfo(this, "Repaired all items, disabling");

                if (didMove) {
                    int emptySlot = getEmptySlot();
                    InvUtils.move().fromOffhand().to(emptySlot);
                }

                toggle();
            }
        }
        else {
            InvUtils.move().from(slot).toOffhand();
            didMove = true;
        }
    }

    private boolean shouldWait() {
        ItemStack itemStack = mc.player.getOffHandStack();

        if (itemStack.isEmpty()) return false;

        if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) > 0) {
            return itemStack.getDamage() != 0;
        }

        return !force.get();
    }

    private int getSlot() {
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (blacklist.get().contains(itemStack.getItem())) continue;

            if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) > 0 && itemStack.getDamage() > 0) {
                return i;
            }
        }

        return -1;
    }

    private int getEmptySlot() {
        for (int i = 0; i < mc.player.inventory.main.size(); i++) {
            if (mc.player.inventory.getStack(i).isEmpty()) return i;
        }

        return -1;
    }
}
