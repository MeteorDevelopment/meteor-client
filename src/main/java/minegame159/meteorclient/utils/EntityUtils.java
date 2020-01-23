package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EnderCrystalEntity;
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
        return entity instanceof EnderCrystalEntity;
    }

    public static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity || entity instanceof AbstractMinecartEntity;
    }
}
