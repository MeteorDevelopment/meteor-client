package me.jellysquid.mods.lithium.common.entity.pushable;

import me.jellysquid.mods.lithium.common.entity.EntityClassGroup;
import me.jellysquid.mods.lithium.common.reflection.ReflectionUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;

public class PushableEntityClassGroup {

    /**
     * Contains Entity Classes that use {@link LivingEntity#isPushable()} ()} to determine their pushability state
     * and use {@link LivingEntity#isClimbing()} to determine their climbing state and are never spectators (no players).
     * <p>
     * LivingEntity, but not Players and not Subclasses with different pushability calculations
     */
    public static final EntityClassGroup CACHABLE_UNPUSHABILITY;

    /**
     * Entities that might be pushable or are always pushable.
     * <p>
     * Pushable subclasses of ArmorStandEntity and BatEntity, Minecarts, Boats, LivingEntity. Not EnderDragonEntity
     */
    public static final EntityClassGroup MAYBE_PUSHABLE;

    static {
        String remapped_isClimbing = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_1309", "method_6101", "()Z");
        String remapped_isPushable = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_1297", "method_5810", "()Z");
        CACHABLE_UNPUSHABILITY = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (LivingEntity.class.isAssignableFrom(entityClass) && !PlayerEntity.class.isAssignableFrom(entityClass)) {
                        if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isPushable)) {
                            if (!ReflectionUtil.hasMethodOverride(entityClass, LivingEntity.class, true, remapped_isClimbing)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
        MAYBE_PUSHABLE = new EntityClassGroup(
                (Class<?> entityClass) -> {
                    if (ReflectionUtil.hasMethodOverride(entityClass, Entity.class, true, remapped_isPushable)) {
                        if (EnderDragonEntity.class.isAssignableFrom(entityClass)) {
                            return false;
                        }
                        if (ArmorStandEntity.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, ArmorStandEntity.class, true, remapped_isPushable);
                        }
                        if (BatEntity.class.isAssignableFrom(entityClass)) {
                            return ReflectionUtil.hasMethodOverride(entityClass, BatEntity.class, true, remapped_isPushable);
                        }
                        return true;
                    }
                    if (PlayerEntity.class.isAssignableFrom(entityClass)) {
                        return true;
                    }
                    return false;
                });
    }
}
