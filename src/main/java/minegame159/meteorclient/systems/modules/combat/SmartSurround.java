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
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

public class SmartSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyObsidian = sgGeneral.add(new BoolSetting.Builder()
            .name("only-obsidian")
            .description("Only uses Obsidian as a surround block.")
            .defaultValue(false)
            .build()
    );

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

    private int oldSlot;
    private int slot = -1;
    private int rPosX;
    private int rPosZ;
    private Entity crystal;

    public SmartSurround(){
        super(Categories.Combat, "smart-surround", "Attempts to save you from crystals automatically.");
    }

    @EventHandler
    private void onSpawn(EntityAddedEvent event) {
        crystal = event.entity;

        if (event.entity.getType() == EntityType.END_CRYSTAL) {
            if (DamageCalcUtils.crystalDamage(mc.player, event.entity.getPos()) > minDamage.get()) {
                slot = findObiInHotbar();

                if (slot == -1 && onlyObsidian.get()) {
                    ChatUtils.moduleError(this, "No obsidian in hotbar... disabling.");
                    return;
                }

                for (int i = 0; i < 9; i++) {
                    Item item = mc.player.inventory.getStack(i).getItem();

                    if (item instanceof BlockItem) {
                        slot = i;
                        mc.player.inventory.selectedSlot = slot;
                        break;
                    }
                }

                if (slot == -1) {
                    ChatUtils.moduleError(this, "No blocks in hotbar... disabling.");
                    return;
                }

                rPosX = mc.player.getBlockPos().getX() - event.entity.getBlockPos().getX();
                rPosZ = mc.player.getBlockPos().getZ() - event.entity.getBlockPos().getZ();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (slot != -1) {
            if ((rPosX >= 2) && (rPosZ == 0)) {
                placeObi(rPosX - 1, 0, crystal);
            } else if ((rPosX > 1) && (rPosZ > 1)) {
                placeObi(rPosX, rPosZ - 1, crystal);
                placeObi(rPosX - 1, rPosZ, crystal);
            } else if ((rPosX == 0) && (rPosZ >= 2)) {
                placeObi(0, rPosZ - 1, crystal);
            } else if ((rPosX < -1) && (rPosZ < -1)) {
                placeObi(rPosX, rPosZ + 1, crystal);
                placeObi(rPosX + 1, rPosZ, crystal);
            } else if ((rPosX == 0) && (rPosZ <= -2)) {
                placeObi(0, rPosZ + 1, crystal);
            } else if ((rPosX > 1) && (rPosZ < -1)) {
                placeObi(rPosX, rPosZ + 1, crystal);
                placeObi(rPosX - 1, rPosZ, crystal);
            } else if ((rPosX <= -2) && (rPosZ == 0)) {
                placeObi(rPosX + 1, 0, crystal);
            } else if ((rPosX < -1) && (rPosZ > 1)) {
                placeObi(rPosX, rPosZ - 1, crystal);
                placeObi(rPosX + 1, rPosZ, crystal);
            }

            if (mc.world.raycast(new RaycastContext(mc.player.getPos(), crystal.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() != HitResult.Type.MISS) {
                slot = -1;
                mc.player.inventory.selectedSlot = oldSlot;
            }
        }
    }

    private void placeObi(int x, int z, Entity crystal) {
        BlockPos blockPos = crystal.getBlockPos().add(x, -1, z);
        BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), 100, true);
    }

    private int findObiInHotbar() {
        oldSlot = mc.player.inventory.selectedSlot;
        int newSlot = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN) {
                newSlot = i;
                mc.player.inventory.selectedSlot = newSlot;
                break;
            }
        }

        return newSlot;
    }
}
