/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class EXPThrower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate downwards when throwing bottles.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-toggle")
            .description("Toggles off when your armor is repaired.")
            .defaultValue(true)
            .build()
    );

    public EXPThrower() {
        super(Categories.Player, "exp-thrower", "Automatically throws XP bottles in your hotbar.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (autoToggle.get()) {
            int count = 0;
            int set = 0;

            for (int i = 0; i < 4; i++) {
                if (!mc.player.inventory.armor.get(i).isEmpty() && EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.inventory.getArmorStack(i)) == 1) set++;
                if (!mc.player.inventory.armor.get(i).isDamaged()) count++;
            }
            if (count == set && set != 0) {
                toggle();
                return;
            }
        }

        int slot = InvUtils.findItemInHotbar(Items.EXPERIENCE_BOTTLE);

        if (slot != -1) {
            if (lookDown.get()) Rotations.rotate(mc.player.yaw, 90, () -> throwExp(slot));
            else throwExp(slot);
        }
    }

    private void throwExp(int slot) {
        int preSelectedSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
        mc.player.inventory.selectedSlot = preSelectedSlot;
    }
}
