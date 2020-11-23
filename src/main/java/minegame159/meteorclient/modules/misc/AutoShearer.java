/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.Hand;

public class AutoShearer extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("Maximum distance.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
    );

    private final Setting<Boolean> preserveBrokenShears = sgGeneral.add(new BoolSetting.Builder()
            .name("preserve-broken-shears")
            .description("Will not break shears.")
            .defaultValue(false)
            .build()
    );

    public AutoShearer() {
        super(Category.Misc, "auto-shearer", "Automatically shears sheeps.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof SheepEntity) || ((SheepEntity) entity).isSheared() || ((SheepEntity) entity).isBaby() || mc.player.distanceTo(entity) > distance.get()) continue;

            boolean findNewShears = false;
            boolean offHand = false;
            if (mc.player.inventory.getMainHandStack().getItem() instanceof ShearsItem) {
                if (preserveBrokenShears.get() && mc.player.inventory.getMainHandStack().getDamage() >= mc.player.inventory.getMainHandStack().getMaxDamage() - 1) findNewShears = true;
            }
            else if (mc.player.inventory.offHand.get(0).getItem() instanceof ShearsItem) {
                if (preserveBrokenShears.get() && mc.player.inventory.offHand.get(0).getDamage() >= mc.player.inventory.offHand.get(0).getMaxDamage() - 1) findNewShears = true;
                else offHand = true;
            }
            else {
                findNewShears = true;
            }

            boolean foundShears = !findNewShears;
            if (findNewShears) {
                int slot = InvUtils.findItemInHotbar(Items.SHEARS, itemStack -> (!preserveBrokenShears.get() || (preserveBrokenShears.get() && itemStack.getDamage() < itemStack.getMaxDamage() - 1)));

                if (slot != -1) {
                    mc.player.inventory.selectedSlot = slot;
                    foundShears = true;
                }
            }

            if (foundShears) {
                mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
                return;
            }
        }
    });
}
