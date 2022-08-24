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
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.Hand;

public class AutoShearer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("The maximum distance the sheep have to be to be sheared.")
            .min(0.0)
            .defaultValue(5.0)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Prevents shears from being broken.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces towards the animal being sheared.")
            .defaultValue(true)
            .build()
    );

    private Entity entity;
    private boolean offHand;

    public AutoShearer() {
        super(Categories.World, "auto-shearer", "Automatically shears sheep.");
    }

    @Override
    public void onDeactivate() {
        entity = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        entity = null;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof SheepEntity) || ((SheepEntity) entity).isSheared() || ((SheepEntity) entity).isBaby() || mc.player.distanceTo(entity) > distance.get()) continue;

            boolean findNewShears = false;
            if (mc.player.getInventory().getMainHandStack().getItem() instanceof ShearsItem) {
                if (antiBreak.get() && mc.player.getInventory().getMainHandStack().getDamage() >= mc.player.getInventory().getMainHandStack().getMaxDamage() - 1) findNewShears = true;
            }
            else if (mc.player.getInventory().offHand.get(0).getItem() instanceof ShearsItem) {
                if (antiBreak.get() && mc.player.getInventory().offHand.get(0).getDamage() >= mc.player.getInventory().offHand.get(0).getMaxDamage() - 1) findNewShears = true;
                else offHand = true;
            }
            else {
                findNewShears = true;
            }

            boolean foundShears = !findNewShears;
            if (findNewShears) {
                FindItemResult shears = InvUtils.findInHotbar(itemStack -> (!antiBreak.get() || (antiBreak.get() && itemStack.getDamage() < itemStack.getMaxDamage() - 1)) && itemStack.getItem() == Items.SHEARS);

                if (InvUtils.swap(shears.slot(), true)) foundShears = true;
            }

            if (foundShears) {
                this.entity = entity;

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity), -100, this::interact);
                else interact();

                return;
            }
        }
    }

    private void interact() {
        mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        InvUtils.swapBack();
    }
}
