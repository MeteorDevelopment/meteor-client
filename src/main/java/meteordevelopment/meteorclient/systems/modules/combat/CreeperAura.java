/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class CreeperAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("automatically switches to flint and steal if in hotbar")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreNamed = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-named")
        .description("Ignore creepers that are named")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The maximum distance the creeper has to be to be ignited.")
        .min(0.0)
        .defaultValue(5.0)
        .build()
    );

    public CreeperAura() {
        super(Categories.Combat, "creeper-aura", "Uses a flint and steal to ignite creepers around you");
    }

    private final ArrayList<CreeperEntity> targets = new ArrayList<>();
    private Hand hand;

    @Override
    public void onDeactivate() {
        targets.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        Item mainHand = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem();
        Item offHand = mc.player.getOffHandStack().getItem();

        // Return early if not needed
        if (!autoSwitch.get() && !(mainHand instanceof FlintAndSteelItem) && !(offHand instanceof FlintAndSteelItem)) return;
        checkEntities();

    }
    public void checkEntities() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof CreeperEntity creeper && creeper.isAlive() && PlayerUtils.isWithin(creeper, distance.get()) && !creeper.isIgnited() && !targets.contains(creeper)) {
                if (ignoreNamed.get() && creeper.hasCustomName()) return;
                targets.add(creeper);
                CreeperEntity currentTarget = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
                checkHands(currentTarget);
            }
        }
    }

    public void checkHands(CreeperEntity currentTarget) {
        Item mainHand = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem();
        Item offHand = mc.player.getOffHandStack().getItem();
        // If player isn't holding flint and steal and auto switch is enabled, switch and then ignite
        if (autoSwitch.get() && !(mainHand instanceof FlintAndSteelItem) && !(offHand instanceof FlintAndSteelItem)) {
            InvUtils.swap(findSlot(), true);
            ignite(currentTarget);
            InvUtils.swapBack();
        }
        if (mainHand instanceof FlintAndSteelItem) {
            hand = Hand.MAIN_HAND;
            ignite(currentTarget);
        }
        if (offHand instanceof FlintAndSteelItem) {
            hand = Hand.OFF_HAND;
            ignite(currentTarget);
        }
    }

    public byte findSlot() {
        byte slot = -1;
        Item offHandItem = mc.player.getOffHandStack().getItem();
        if (offHandItem instanceof FlintAndSteelItem) {
            slot = SlotUtils.OFFHAND;
            hand = Hand.OFF_HAND;
            return slot;
        }

        for (byte i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof FlintAndSteelItem) {
                slot = i;
                hand = Hand.MAIN_HAND;
                return slot;
            }
        }
        return slot;
    }

    public void ignite(CreeperEntity target) {
        mc.interactionManager.interactEntity(mc.player, target, hand);
        targets.remove(target);
    }
}
