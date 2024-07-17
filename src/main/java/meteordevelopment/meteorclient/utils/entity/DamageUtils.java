/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("JavadocReference")
public class DamageUtils {
    private DamageUtils() {
    }

    // Explosion damage

    /**
     * It is recommended to use this {@link RaycastFactory} unless you implement custom behaviour, as soon:tm: it will be the
     * target of optimizations to make it more performant.
     * @see BlockView#raycast(RaycastContext)
     */
    public static final RaycastFactory HIT_FACTORY = (context, blockPos) -> {
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (blockState.getBlock().getBlastResistance() < 600) return null;

        return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
    };

    public static float crystalDamage(LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 12f, raycastFactory);
    }

    public static float bedDamage(LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastFactory);
    }

    public static float anchorDamage(LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, RaycastFactory raycastFactory) {
        return explosionDamage(target, targetPos, targetBox, explosionPos, 10f, raycastFactory);
    }

    /**
     * Low level control of parameters without having to reimplement everything, for addon authors who wish to use their
     * own predictions or other systems.
     * @see net.minecraft.world.explosion.ExplosionBehavior#calculateDamage(Explosion, Entity)
     */
    public static float explosionDamage(LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, float power, RaycastFactory raycastFactory) {
        double modDistance = PlayerUtils.distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
        if (modDistance > power) return 0f;

        double exposure = getExposure(explosionPos, targetBox, raycastFactory);
        double impact = (1 - (modDistance / power)) * exposure;
        float damage = (int) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return calculateReductions(damage, target, mc.world.getDamageSources().explosion(null));
    }

    /** Meteor Client implementations */

    public static float crystalDamage(LivingEntity target, Vec3d crystal, boolean predictMovement, BlockPos obsidianPos) {
        return overridingExplosionDamage(target, crystal, 12f, predictMovement, obsidianPos, Blocks.OBSIDIAN.getDefaultState());
    }

    public static float crystalDamage(LivingEntity target, Vec3d crystal) {
        return explosionDamage(target, crystal, 12f, false);
    }

    public static float bedDamage(LivingEntity target, Vec3d bed) {
        return explosionDamage(target, bed, 10f, false);
    }

    public static float anchorDamage(LivingEntity target, Vec3d anchor) {
        return overridingExplosionDamage(target, anchor, 10f, false, BlockPos.ofFloored(anchor), Blocks.AIR.getDefaultState());
    }

    private static float overridingExplosionDamage(LivingEntity target, Vec3d explosionPos, float power, boolean predictMovement, BlockPos overridePos, BlockState overrideState) {
        return explosionDamage(target, explosionPos, power, predictMovement, getOverridingHitFactory(overridePos, overrideState));
    }

    private static float explosionDamage(LivingEntity target, Vec3d explosionPos, float power, boolean predictMovement) {
        return explosionDamage(target, explosionPos, power, predictMovement, HIT_FACTORY);
    }

    private static float explosionDamage(LivingEntity target, Vec3d explosionPos, float power, boolean predictMovement, RaycastFactory raycastFactory) {
        if (target == null) return 0f;
        if (target instanceof PlayerEntity player && EntityUtils.getGameMode(player) == GameMode.CREATIVE && !(player instanceof FakePlayerEntity)) return 0f;

        Vec3d position = predictMovement ? target.getPos().add(target.getVelocity()) : target.getPos();

        Box box = target.getBoundingBox();
        if (predictMovement) box = box.offset(target.getVelocity());

        return explosionDamage(target, position, box, explosionPos, power, raycastFactory);
    }

    public static RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
        return (context, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(overridePos)) blockState = overrideState;
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600) return null;
            }

            return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
        };
    }

    // Sword damage

    /**
     * @see PlayerEntity#attack(Entity)
     */
    public static float getAttackDamage(LivingEntity attacker, LivingEntity target) {
        float itemDamage = (float) attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        DamageSource damageSource = attacker instanceof PlayerEntity player ? mc.world.getDamageSources().playerAttack(player) : mc.world.getDamageSources().mobAttack(attacker);

        // Get enchant damage
        ItemStack stack = attacker.getWeaponStack();
        float enchantDamage = /*fixme EnchantmentHelper.getDamage(attacker.getWorld() instanceof ServerWorld serverWorld ? serverWorld : null, stack, target, damageSource, itemDamage) - itemDamage*/ 0;

        // Factor charge
        if (attacker instanceof PlayerEntity playerEntity) {
            float charge = playerEntity.getAttackCooldownProgress(0.5f);
            itemDamage *= 0.2f + charge * charge * 0.8f;
            enchantDamage *= charge;

            // Factor critical hit
            if (charge > 0.9f && attacker.fallDistance > 0f && !attacker.isOnGround() && !attacker.isClimbing() && !attacker.isTouchingWater() && !attacker.hasStatusEffect(StatusEffects.BLINDNESS) && !attacker.hasVehicle()) {
                itemDamage *= 1.5f;
            }
        }

        float damage = itemDamage + enchantDamage;

        damage = calculateReductions(damage, target, damageSource);

        return damage;
    }

    // Fall Damage

    /**
     * @see LivingEntity#computeFallDamage(float, float) (float, float, DamageSource)
     */
    public static float fallDamage(LivingEntity entity) {
        if (entity instanceof PlayerEntity player && player.getAbilities().flying) return 0f;
        if (entity.hasStatusEffect(StatusEffects.SLOW_FALLING) || entity.hasStatusEffect(StatusEffects.LEVITATION)) return 0f;

        // Fast path - Above the surface
        int surface = mc.world.getWorldChunk(entity.getBlockPos()).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(entity.getBlockX() & 15, entity.getBlockZ() & 15);
        if (entity.getBlockY() >= surface) return fallDamageReductions(entity, surface);

        // Under the surface
        BlockHitResult raycastResult = mc.world.raycast(new RaycastContext(entity.getPos(), new Vec3d(entity.getX(), mc.world.getBottomY(), entity.getZ()), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.WATER, entity));
        if (raycastResult.getType() == HitResult.Type.MISS) return 0;

        return fallDamageReductions(entity, raycastResult.getBlockPos().getY());
    }

    private static float fallDamageReductions(LivingEntity entity, int surface) {
        int fallHeight = (int) (entity.getY() - surface + entity.fallDistance - 3d);
        @Nullable StatusEffectInstance jumpBoostInstance = entity.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (jumpBoostInstance != null) fallHeight -= jumpBoostInstance.getAmplifier() + 1;

        return calculateReductions(fallHeight, entity, mc.world.getDamageSources().fall());
    }

    // Utils

    /**
     * @see LivingEntity#applyDamage(DamageSource, float)
     */
    public static float calculateReductions(float damage, LivingEntity entity, DamageSource damageSource) {
        if (damageSource.isScaledWithDifficulty()) {
            switch (mc.world.getDifficulty()) {
                case EASY     -> damage = Math.min(damage / 2 + 1, damage);
                case HARD     -> damage *= 1.5f;
            }
        }

        // Armor reduction
        damage = DamageUtil.getDamageLeft(entity, damage, damageSource, getArmor(entity), (float) entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));

        // Resistance reduction
        damage = resistanceReduction(entity, damage);

        // Protection reduction
        damage = protectionReduction(entity, damage, damageSource);

        return Math.max(damage, 0);
    }

    private static float getArmor(LivingEntity entity) {
        return (float) Math.floor(entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
    }

    /**
     * @see LivingEntity#modifyAppliedDamage(DamageSource, float)
     */
    private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
        //fixme float protLevel = EnchantmentHelper.getProtectionAmount(player.getWorld() instanceof ServerWorld serverWorld ? serverWorld : null, player, source);
        return DamageUtil.getInflictedDamage(damage, /*protLevel*/ 0);
    }

    /**
     * @see LivingEntity#modifyAppliedDamage(DamageSource, float)
     */
    private static float resistanceReduction(LivingEntity player, float damage) {
        StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1 - (lvl * 0.2f));
        }

        return Math.max(damage, 0);
    }

    /**
     * @see Explosion#getExposure(Vec3d, Entity)
     */
    private static float getExposure(Vec3d source, Box box, RaycastFactory raycastFactory) {
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1 / (xDiff * 2 + 1);
        double yStep = 1 / (yDiff * 2 + 1);
        double zStep = 1 / (zDiff * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) * 0.5;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d position = new Vec3d(x, y, z);

                        if (raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) misses++;

                        hits++;
                    }
                }
            }

            return (float) misses / hits;
        }

        return 0f;
    }

    /* Raycasts */

    private static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
        return BlockView.raycast(context.start, context.end, context, raycastFactory, ctx -> null);
    }

    public record ExposureRaycastContext(Vec3d start, Vec3d end) {}

    @FunctionalInterface
    public interface RaycastFactory extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {}
}
