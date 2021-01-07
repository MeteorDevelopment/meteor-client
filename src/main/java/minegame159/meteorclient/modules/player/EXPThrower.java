/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

public class EXPThrower extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder()
            .name("look-down")
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
        super(Category.Player, "exp-thrower", "Automatically throws XP bottles in your hotbar.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            if (lookDown.get()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(mc.player.yaw, 90, mc.player.isOnGround()));
            }
            int preSelectedSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.inventory.selectedSlot = preSelectedSlot;
        }

        if(autoToggle.get()) {
            int count = 0;
            int set = 0;

            for(int i = 0; i < 4; i++) {
                if(!mc.player.inventory.armor.get(i).isEmpty() && EnchantmentHelper.getLevel(Enchantments.MENDING, mc.player.inventory.getArmorStack(i)) == 1) set++;
                if(!mc.player.inventory.armor.get(i).isDamaged()) count++;
            }
            if(count == set && set != 0) toggle();
        }
    });
}
