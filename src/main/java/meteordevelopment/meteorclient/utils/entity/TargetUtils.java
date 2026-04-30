/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TargetUtils {
    private static final List<Entity> ENTITIES = new ArrayList<>();

    private TargetUtils() {
    }

    @Nullable
    public static Entity get(Predicate<Entity> isGood, SortPriority sortPriority) {
        ENTITIES.clear();
        getList(ENTITIES, isGood, sortPriority, 1);
        if (!ENTITIES.isEmpty()) {
            return ENTITIES.getFirst();
        }

        return null;
    }

    public static void getList(List<Entity> targetList, Predicate<Entity> isGood, SortPriority sortPriority, int maxCount) {
        targetList.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != null && isGood.test(entity)) targetList.add(entity);
        }

        FakePlayerManager.forEach(fp -> {
            if (fp != null && isGood.test(fp)) targetList.add(fp);
        });

        targetList.sort(sortPriority);
        // fast list trimming
        if (targetList.size() > maxCount) {
            targetList.subList(maxCount, targetList.size()).clear();
        }
    }

    @Nullable
    public static Player getPlayerTarget(double range, SortPriority priority) {
        if (!Utils.canUpdate()) return null;
        return (Player) get(entity -> {
            if (!(entity instanceof Player player) || entity == mc.player) return false;
            if (player.isDeadOrDying() || player.getHealth() <= 0) return false;
            if (!PlayerUtils.isWithin(entity, range)) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (entity instanceof FakePlayerEntity fakePlayer) return !fakePlayer.noHit;
            return EntityUtils.getGameMode(player) == GameType.SURVIVAL;
        }, priority);
    }

    public static boolean isBadTarget(Player target, double range) {
        if (target == null) return true;
        return !PlayerUtils.isWithin(target, range) || !target.isAlive() || target.isDeadOrDying() || target.getHealth() <= 0;
    }
}
