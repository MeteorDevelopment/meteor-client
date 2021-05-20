package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static minegame159.meteorclient.utils.Utils.mc;

public class TargetUtils {
    private static final List<Entity> ENTITIES = new ArrayList<>();

    public static Entity get(Predicate<Entity> isGood, SortPriority sortPriority) {
        ENTITIES.clear();
        getList(isGood, sortPriority, ENTITIES, 1);
        if (!ENTITIES.isEmpty()) {
            return ENTITIES.get(0);
        }

        return null;
    }

    public static void getList(Predicate<Entity> isGood, SortPriority sortPriority, List<Entity> targetList, int maxCount) {
        targetList.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (isGood.test(entity)) targetList.add(entity);
        }

        for (Entity entity : FakePlayerManager.getPlayers()) {
            if (isGood.test(entity)) targetList.add(entity);
        }

        targetList.sort((e1, e2) -> sort(e1, e2, sortPriority));
        targetList.removeIf(entity -> targetList.indexOf(entity) > maxCount -1);
    }

    public static PlayerEntity getPlayerTarget(double range, SortPriority priority) {
        if (!Utils.canUpdate()) return null;
        return (PlayerEntity) get(entity -> {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (mc.player.distanceTo(entity) > range) return false;
            if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            return EntityUtils.getGameMode((PlayerEntity) entity) == GameMode.SURVIVAL || entity instanceof FakePlayerEntity;
        }, priority);
    }

    public static boolean isBadTarget(PlayerEntity target, double range) {
        if (target == null) return true;
        return mc.player.distanceTo(target) > range || !target.isAlive() || target.isDead() || target.getHealth() <= 0;
    }

    private static int sort(Entity e1, Entity e2, SortPriority priority) {
        switch (priority) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth:    return sortHealth(e1, e2);
            case HighestHealth:   return invertSort(sortHealth(e1, e2));
            case ClosestAngle:    return sortAngle(e1, e2);
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

    private static int sortAngle(Entity e1, Entity e2) {
        boolean e1l = e1 instanceof LivingEntity;
        boolean e2l = e2 instanceof LivingEntity;

        if (!e1l && !e2l) return 0;
        else if (e1l && !e2l) return 1;
        else if (!e1l) return -1;

        double e1yaw = Math.abs(Rotations.getYaw(e1) - mc.player.yaw);
        double e2yaw = Math.abs(Rotations.getYaw(e2) - mc.player.yaw);

        double e1pitch = Math.abs(Rotations.getPitch(e1) - mc.player.pitch);
        double e2pitch = Math.abs(Rotations.getPitch(e2) - mc.player.pitch);

        return Double.compare(Math.sqrt(e1yaw * e1yaw + e1pitch * e1pitch), Math.sqrt(e2yaw * e2yaw + e2pitch * e2pitch));
    }

    private static int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }

}
