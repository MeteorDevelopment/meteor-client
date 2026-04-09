/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class AutoMend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> blacklist = sgGeneral.add(new ItemListSetting.Builder()
        .name("blacklist")
        .description("Item blacklist.")
        .filter(item -> item.components().get(DataComponents.DAMAGE) != null)
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
                info("Repaired all items, disabling");

                if (didMove) {
                    int emptySlot = getEmptySlot();
                    InvUtils.move().fromOffhand().to(emptySlot);
                }

                toggle();
            }
        } else {
            InvUtils.move().from(slot).toOffhand();
            didMove = true;
        }
    }

    private boolean shouldWait() {
        ItemStack itemStack = mc.player.getOffhandItem();

        if (itemStack.isEmpty()) return false;

        if (Utils.hasEnchantments(itemStack, Enchantments.MENDING)) {
            return itemStack.getDamageValue() != 0;
        }

        return !force.get();
    }

    private int getSlot() {
        for (int i = 0; i < mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (blacklist.get().contains(itemStack.getItem())) continue;

            if (Utils.hasEnchantments(itemStack, Enchantments.MENDING) && itemStack.getDamageValue() > 0) {
                return i;
            }
        }

        return -1;
    }

    private int getEmptySlot() {
        for (int i = 0; i < mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }

        return -1;
    }
}
