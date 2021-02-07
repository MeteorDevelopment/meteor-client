/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.text.TextUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtils {
    public static MinecraftClient mc;
    public static List<Entity> entities = new ArrayList<>();

    public static boolean isAttackable(EntityType<?> type) {
        return type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.ARROW && type != EntityType.FALLING_BLOCK && type != EntityType.FIREWORK_ROCKET && type != EntityType.ITEM && type != EntityType.LLAMA_SPIT && type != EntityType.SPECTRAL_ARROW && type != EntityType.ENDER_PEARL && type != EntityType.EXPERIENCE_BOTTLE && type != EntityType.POTION && type != EntityType.TRIDENT && type != EntityType.LIGHTNING_BOLT && type != EntityType.FISHING_BOBBER && type != EntityType.EXPERIENCE_ORB && type != EntityType.EGG;
    }

    public static Color getEntityColor(Entity entity, Color players, Color animals, Color waterAnmals, Color monsters, Color ambient, Color misc, boolean useNameColor) {
        if (entity instanceof PlayerEntity) {
            Color friendColor = Friends.get().getFriendColor((PlayerEntity) entity);

            if (friendColor != null) return new Color(friendColor.r, friendColor.g, friendColor.b, players.a);
            else if (useNameColor) return TextUtils.getMostPopularColor(entity.getDisplayName());
            else return players;
        }

        switch (entity.getType().getSpawnGroup()) {
            case CREATURE:       return animals;
            case WATER_CREATURE: return waterAnmals;
            case MONSTER:        return monsters;
            case AMBIENT:        return ambient;
            case MISC:           return misc;
        }

        return Utils.WHITE;
    }

    public static Entity get(Predicate<Entity> isGood, SortPriority sortPriority) {
        for (Entity entity : mc.world.getEntities()) {
            if (isGood.test(entity)) entities.add(entity);
        }

        for (Entity entity : FakePlayerUtils.getPlayers().keySet()) {
            if (isGood.test(entity)) entities.add(entity);
        }

        entities.sort((e1, e2) -> sort(e1, e2, sortPriority));

        if (!entities.isEmpty()) {
            Entity res = entities.get(0);

            entities.clear();
            return res;
        }

        return null;
    }

    private static int sort(Entity e1, Entity e2, SortPriority priority) {
        switch (priority) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth:    return sortHealth(e1, e2);
            case HighestHealth:   return invertSort(sortHealth(e1, e2));
            default:              return 0;
        }
    }

    private static int sortHealth(Entity e1, Entity e2) {
        boolean e1l = e1 instanceof LivingEntity;
        boolean e2l = e2 instanceof LivingEntity;

        if (!e1l && !e2l) return 0;
        else if (e1l && !e2l) return 1;
        else if (!e1l) return -1;

        return Float.compare(((LivingEntity) e1).getHealth(), ((LivingEntity) e2).getHealth());
    }

    private static int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

    public static float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    public static boolean isInvalid(PlayerEntity target, double range) {
        if (target == null) return true;
        return mc.player.distanceTo(target) > range || !target.isAlive() || target.isDead() || target.getHealth() <= 0;
    }

    public static PlayerEntity getPlayerTarget(double range, SortPriority priority) {
        if (!Utils.canUpdate()) return null;
        return (PlayerEntity) get(entity -> {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (mc.player.distanceTo(entity) > range) return false;
            if (!Friends.get().attack((PlayerEntity) entity)) return false;
            return entity instanceof FakePlayerEntity || (!((PlayerEntity) entity).isCreative() && !entity.isSpectator());
        }, priority);
    }

    public static int getPing(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }

    public static GameMode getGameMode(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return null;
        return playerListEntry.getGameMode();
    }
}
