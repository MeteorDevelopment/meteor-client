/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

//Created by squidoodly 15/04/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.player.FindItemResult;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;

public class SmartSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage before this activates.")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate towards the blocks being placed.")
            .defaultValue(true)
            .build()
    );

    private int rPosX;
    private int rPosZ;
    private Entity crystal;

    public SmartSurround() {
        super(Categories.Combat, "smart-surround", "Attempts to save you from crystals automatically.");
    }

    @EventHandler
    private void onSpawn(EntityAddedEvent event) {
        crystal = event.entity;

        if (event.entity.getType() == EntityType.END_CRYSTAL) {
            if (DamageCalcUtils.crystalDamage(mc.player, event.entity.getPos()) > minDamage.get()) {
                rPosX = mc.player.getBlockPos().getX() - event.entity.getBlockPos().getX();
                rPosZ = mc.player.getBlockPos().getZ() - event.entity.getBlockPos().getZ();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);

        if (!obsidian.found()) return;

        int prevSlot = mc.player.inventory.selectedSlot;

        if ((rPosX >= 2) && (rPosZ == 0)) {
            placeObi(crystal, rPosX - 1, 0, obsidian);
        } else if ((rPosX > 1) && (rPosZ > 1)) {
            placeObi(crystal, rPosX, rPosZ - 1, obsidian);
            placeObi(crystal, rPosX - 1, rPosZ, obsidian);
        } else if ((rPosX == 0) && (rPosZ >= 2)) {
            placeObi(crystal, 0, rPosZ - 1, obsidian);
        } else if ((rPosX < -1) && (rPosZ < -1)) {
            placeObi(crystal, rPosX, rPosZ + 1, obsidian);
            placeObi(crystal, rPosX + 1, rPosZ, obsidian);
        } else if ((rPosX == 0) && (rPosZ <= -2)) {
            placeObi(crystal, 0, rPosZ + 1, obsidian);
        } else if ((rPosX > 1) && (rPosZ < -1)) {
            placeObi(crystal, rPosX, rPosZ + 1, obsidian);
            placeObi(crystal, rPosX - 1, rPosZ, obsidian);
        } else if ((rPosX <= -2) && (rPosZ == 0)) {
            placeObi(crystal, rPosX + 1, 0, obsidian);
        } else if ((rPosX < -1) && (rPosZ > 1)) {
            placeObi(crystal, rPosX, rPosZ - 1, obsidian);
            placeObi(crystal, rPosX + 1, rPosZ, obsidian);
        }

        InvUtils.swap(prevSlot);
    }

    private void placeObi(Entity crystal, int x, int z, FindItemResult findItemResult) {
        BlockUtils.place(crystal.getBlockPos().add(x, -1, z), findItemResult, rotate.get(), 100, false, true, false);
    }
}
