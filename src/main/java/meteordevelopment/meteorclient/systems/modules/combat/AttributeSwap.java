/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;

public class AttributeSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWeapon = settings.createGroup("Weapon Options");

    private final Setting<Integer> targetSlot = sgGeneral.add(new IntSetting.Builder()
        .name("target-slot")
        .description("Hotbar slot to swap to (1-9).")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 9)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to the original slot after a delay.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapBackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-back-delay")
        .description("Delay in ticks before swapping back.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 20)
        .visible(swapBack::get)
        .build()
    );

    private final Setting<Boolean> onlyOnWeapon = sgWeapon.add(new BoolSetting.Builder()
        .name("only-on-weapon")
        .description("Only swaps when holding a selected weapon in hand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sword = sgWeapon.add(new BoolSetting.Builder()
        .name("sword")
        .description("Works while holding a sword.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> axe = sgWeapon.add(new BoolSetting.Builder()
        .name("axe")
        .description("Works while holding an axe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> pickaxe = sgWeapon.add(new BoolSetting.Builder()
        .name("pickaxe")
        .description("Works while holding a pickaxe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> shovel = sgWeapon.add(new BoolSetting.Builder()
        .name("shovel")
        .description("Works while holding a shovel.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> hoe = sgWeapon.add(new BoolSetting.Builder()
        .name("hoe")
        .description("Works while holding a hoe.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private final Setting<Boolean> mace = sgWeapon.add(new BoolSetting.Builder()
        .name("mace")
        .description("Works while holding a mace.")
        .defaultValue(true)
        .visible(onlyOnWeapon::get)
        .build()
    );

    private int backTimer;
    private boolean awaitingBack;

    public AttributeSwap() {
        super(Categories.Combat, "attribute-swap", "Swaps to a target slot when you attack.");
    }

    @Override
    public void onDeactivate() {
        backTimer = 0;
        awaitingBack = false;
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!canSwapByWeapon() || awaitingBack) return;

        int slotIndex = targetSlot.get() - 1;
        if (slotIndex < 0 || slotIndex > 8) return;
        if (!InvUtils.swap(slotIndex, swapBack.get())) return;

        awaitingBack = swapBack.get();
        if (awaitingBack) backTimer = swapBackDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!awaitingBack) return;
        if (backTimer-- > 0) return;
        InvUtils.swapBack();
        awaitingBack = false;
    }

    private boolean canSwapByWeapon() {
        if (!onlyOnWeapon.get()) return true;
        return InvUtils.testInMainHand(itemStack -> {
            if (sword.get() && itemStack.isIn(ItemTags.SWORDS)) return true;
            if (axe.get() && itemStack.isIn(ItemTags.AXES)) return true;
            if (pickaxe.get() && itemStack.isIn(ItemTags.PICKAXES)) return true;
            if (shovel.get() && itemStack.isIn(ItemTags.SHOVELS)) return true;
            if (hoe.get() && itemStack.isIn(ItemTags.HOES)) return true;
            if (mace.get() && itemStack.getItem() instanceof MaceItem) return true;
            return false;
        });
    }
}


