/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Created by squidoodly 06/07/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.MiddleMouseButtonEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class MiddleClickExtra extends Module {
    public enum Mode {
        Pearl(Items.ENDER_PEARL),
        Bow(Items.ARROW),
        Gap(Items.GOLDEN_APPLE),
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Rod(Items.FISHING_ROD);

        private final Item item;

        Mode(Item item){this.item = item;}
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which item to use when you middle click.")
            .defaultValue(Mode.Pearl)
            .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
            .name("notify")
            .description("Notifies you when you do not have the specified item in your hotbar.")
            .defaultValue(true)
            .build()
    );

    // Item specification errors.

    private boolean wasUsing = false;
    private int preSlot;
    private int preCount;

    public MiddleClickExtra(){
        super(Categories.Player, "middle-click-extra", "Lets you use items when you middle click. Works the same as Middle Click Friend.");
    }

    @EventHandler
    private void onMiddleMouse(MiddleMouseButtonEvent event) {
        InvUtils.FindItemResult result;
        switch(mode.get()){
            case Pearl: {
                result = InvUtils.findItemWithCount(Items.ENDER_PEARL);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                    mc.player.inventory.selectedSlot = preSlot;
                } else if (notify.get()) {
                    ChatUtils.moduleWarning(this, "Unable to find specified item.");
                }
                break;
            }case Gap: {
                result = InvUtils.findItemWithCount(Items.GOLDEN_APPLE);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    preCount = result.count;
                    mc.options.keyUse.setPressed(true);
                    wasUsing = true;
                } else if(notify.get()) {
                    ChatUtils.moduleWarning(this, "Unable to find specified item.");
                }
                break;
            }case EGap:{
                result = InvUtils.findItemWithCount(Items.ENCHANTED_GOLDEN_APPLE);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    preCount = result.count;
                    mc.options.keyUse.setPressed(true);
                    wasUsing = true;
                } else if(notify.get()) {
                    ChatUtils.moduleWarning(this, "Unable to find selected item.");
                }
                break;
            }case Bow:{
                result = InvUtils.findItemWithCount(Items.BOW);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    result = InvUtils.findItemWithCount(Items.ARROW);
                    preCount = result.count;
                    wasUsing = true;
                } else if(notify.get()) {
                    ChatUtils.moduleWarning(this, "Unable to find specified item.");
                }
                break;
            }case Rod: {
                result = InvUtils.findItemWithCount(Items.FISHING_ROD);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                } else if (notify.get()) {
                    ChatUtils.moduleWarning(this, "Unable to find specified item.");
                }
                break;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!wasUsing) return;

        if (preCount > InvUtils.findItemWithCount(mode.get().item).count || (mc.player.getMainHandStack().getItem() != mode.get().item && (mode.get() == Mode.Bow && mc.player.getMainHandStack().getItem() != Items.BOW))){
            mc.options.keyUse.setPressed(false);
            mc.player.inventory.selectedSlot = preSlot;
            wasUsing = false;
        } else {
            mc.options.keyUse.setPressed(true);
        }
    }
}
