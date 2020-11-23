/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Created by squidoodly 06/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.MiddleMouseButtonEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class MiddleClickExtra extends ToggleModule {
    public enum Mode{
        Pearl(Items.ENDER_PEARL),
        Bow(Items.ARROW),
        Gap(Items.GOLDEN_APPLE),
        EGap(Items.ENCHANTED_GOLDEN_APPLE),
        Rod(Items.FISHING_ROD);

        private final Item item;

        Mode(Item item){this.item = item;}
    }

    public MiddleClickExtra(){
        super(Category.Player, "middle-click-extra", "Lets you use items on middle click (works at the same time as Middle Click Friend).");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("What to do when you middle click.")
            .defaultValue(Mode.Pearl)
            .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
            .name("notify")
            .description("Notify you when you don't have the selected item in your hotbar")
            .defaultValue(true)
            .build()
    );

    private boolean wasUsing = false;
    private int preSlot;
    private int preCount;
    private InvUtils.FindItemResult result;

    @EventHandler
    private final Listener<MiddleMouseButtonEvent> onMiddleMouse = new Listener<>(event -> {
        switch(mode.get()){
            case Pearl: {
                result = InvUtils.findItemWithCount(Items.ENDER_PEARL);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                    mc.player.inventory.selectedSlot = preSlot;
                } else if (notify.get()) {
                    Chat.error(this, "Unable to find selected item.");
                }
                break;
            }case Gap: {
                result = InvUtils.findItemWithCount(Items.GOLDEN_APPLE);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    preCount = result.count;
                    ((IKeyBinding) mc.options.keyUse).setPressed(true);
                    wasUsing = true;
                } else if(notify.get()) {
                    Chat.error(this, "Unable to find selected item.");
                }
                break;
            }case EGap:{
                result = InvUtils.findItemWithCount(Items.ENCHANTED_GOLDEN_APPLE);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    preCount = result.count;
                    ((IKeyBinding) mc.options.keyUse).setPressed(true);
                    wasUsing = true;
                } else if(notify.get()) {
                    Chat.error(this, "Unable to find selected item.");
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
                    Chat.error(this, "Unable to find selected item.");
                }
                break;
            }case Rod: {
                result = InvUtils.findItemWithCount(Items.FISHING_ROD);
                if (result.slot <= 8 && result.slot != -1) {
                    preSlot = mc.player.inventory.selectedSlot;
                    mc.player.inventory.selectedSlot = result.slot;
                    mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
                } else if (notify.get()) {
                    Chat.error(this, "Unable to find selected item.");
                }
                break;
            }
        }
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if(!wasUsing) return;
        if(preCount > InvUtils.findItemWithCount(mode.get().item).count || (mc.player.getMainHandStack().getItem() != mode.get().item
                && (mode.get() == Mode.Bow && mc.player.getMainHandStack().getItem() != Items.BOW))){
            ((IKeyBinding) mc.options.keyUse).setPressed(false);
            mc.player.inventory.selectedSlot = preSlot;
            wasUsing = false;
        } else {
            ((IKeyBinding) mc.options.keyUse).setPressed(true);
        }
    });
}
