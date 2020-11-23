/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;

public class EntityUtils {
    public static MinecraftClient mc;

    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    public static boolean isAnimal(Entity entity) {
        return entity instanceof AnimalEntity || entity instanceof AmbientEntity || entity instanceof WaterCreatureEntity || entity instanceof GolemEntity || entity instanceof VillagerEntity;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof Monster;
    }

    public static boolean isItem(Entity entity) {
        return entity instanceof ItemEntity;
    }

    public static boolean isCrystal(Entity entity) {
        return entity instanceof EndCrystalEntity;
    }

    public static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof AbstractMinecartEntity;
    }

    public static boolean isAttackable(EntityType<?> type) {
        return type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.ARROW && type != EntityType.FALLING_BLOCK && type != EntityType.FIREWORK_ROCKET && type != EntityType.ITEM && type != EntityType.LLAMA_SPIT && type != EntityType.SPECTRAL_ARROW && type != EntityType.ENDER_PEARL && type != EntityType.EXPERIENCE_BOTTLE && type != EntityType.POTION && type != EntityType.TRIDENT && type != EntityType.LIGHTNING_BOLT && type != EntityType.FISHING_BOBBER && type != EntityType.EXPERIENCE_ORB && type != EntityType.EGG;
    }
}
