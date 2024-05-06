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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ChestSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Chestplate> chestplate = sgGeneral.add(new EnumSetting.Builder<Chestplate>()
        .name("chestplate")
        .description("Which type of chestplate to swap to.")
        .defaultValue(Chestplate.Best)
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
        .description("Sends inventory close packet after swap.")
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

    private Map<Integer, Map<ArmorItem, Integer>> getChestplates() {
        Map<Integer, Map<ArmorItem, Integer>> chestplates = new HashMap<>();

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            Item item = mc.player.getInventory().main.get(i).getItem();

            if (item instanceof ArmorItem armorItem && ((ArmorItem) item).getSlotType() == EquipmentSlot.CHEST) {
                int armorProtection = armorItem.getProtection();
                Map<ArmorItem, Integer> armorInfo = new HashMap<>();
                armorInfo.put(armorItem, armorProtection);
                chestplates.put(i, armorInfo);
            }
        }
        return chestplates;
    }

    private boolean equipChestplate() {
        int bestSlot = -1;

        Map<Integer, Map<ArmorItem, Integer>> chestplates = getChestplates().entrySet().stream()
            .sorted(Map.Entry.<Integer, Map<ArmorItem, Integer>>comparingByValue(
                Comparator.comparingInt(armorInfo -> armorInfo.values().iterator().next())).reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
            ));

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            Item item = mc.player.getInventory().main.get(i).getItem();

            switch (chestplate.get()) {
                case Leather:
                    if (item == Items.LEATHER_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Chainmail:
                    if (item == Items.CHAINMAIL_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Gold:
                    if (item == Items.GOLDEN_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Iron:
                    if (item == Items.IRON_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Diamond:
                    if (item == Items.DIAMOND_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Netherite:
                    if (item == Items.NETHERITE_CHESTPLATE) {
                        bestSlot = i;
                    }
                    break;
                case Best:
                    Map.Entry<Integer, Map<ArmorItem, Integer>> firstEntry = chestplates.entrySet().iterator().next();
                    bestSlot = firstEntry.getKey();
                    break;
            }
        }
        if (bestSlot != -1) equip(bestSlot);
        return bestSlot != -1;
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
        Leather,
        Chainmail,
        Gold,
        Iron,
        Diamond,
        Netherite,
        Best
    }
}
