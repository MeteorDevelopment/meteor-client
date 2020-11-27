/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Created by squidoodly 15/04/2020
//Added by squidoodly 18/04/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

public class SmartSurround extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyObsidian = sgGeneral.add(new BoolSetting.Builder()
            .name("only-obsidian")
            .description("Only uses Obsidian")
            .defaultValue(false)
            .build());

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage before this activates.")
            .defaultValue(5.5)
            .build());

    private int oldSlot;

    private int slot = -1;

    private int rPosX;

    private int rPosZ;

    private Entity crystal;

    public SmartSurround(){
        super(Category.Combat, "smart-surround", "Tries to save you from crystals automatically.");
    }

    @EventHandler
    private final Listener<EntityAddedEvent> onSpawn = new Listener<>(event -> {
        crystal = event.entity;

        if (event.entity.getType() == EntityType.END_CRYSTAL) {
            if (DamageCalcUtils.crystalDamage(mc.player, event.entity.getPos()) > minDamage.get()) {
                slot = findObiInHotbar();

                if (slot == -1 && onlyObsidian.get()) {
                    Chat.warning(this, "No Obsidian in hotbar. Disabling!");
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
                    Chat.warning(this, "No blocks in hotbar. Disabling!");
                    return;
                }

                rPosX = mc.player.getBlockPos().getX() - event.entity.getBlockPos().getX();
                rPosZ = mc.player.getBlockPos().getZ() - event.entity.getBlockPos().getZ();
            }
        }
    });

    @EventHandler
    private final  Listener<PostTickEvent> onTick = new Listener<>(event -> {
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
    });

    private void placeObi(int x, int z, Entity crystal) {
        PlayerUtils.placeBlock(crystal.getBlockPos().add(x, -1, z), Hand.MAIN_HAND);
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
