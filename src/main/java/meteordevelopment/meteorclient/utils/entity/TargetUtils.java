/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.systems.targeting.Targeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Deprecated // systems.targeting.Targeting
public class TargetUtils {
    private TargetUtils() {

    }

    @Nullable
    public static Entity get(Predicate<Entity> isGood, SortPriority sortPriority) {
        return Targeting.findTarget(sortPriority, isGood);
    }


    public static void getList(List<Entity> targetList, Predicate<Entity> isGood, SortPriority sortPriority, int maxCount) {
        Targeting.findTargets(targetList, isGood, sortPriority, maxCount);
    }

    @Nullable
    public static PlayerEntity getPlayerTarget(double range, SortPriority priority) {
        return Targeting.findPlayerTarget(range, priority);
    }


    public static boolean isBadTarget(PlayerEntity target, double range) {
        return !Targeting.isValidPlayerTarget(target, range);
    }
}
