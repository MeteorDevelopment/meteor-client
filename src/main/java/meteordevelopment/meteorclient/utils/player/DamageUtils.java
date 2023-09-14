/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
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
import net.minecraft.world.explosion.Explosion;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DamageUtils {
    private static DamageSource damageSource;

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(DamageUtils.class);
    }

    @EventHandler
    private static void onGameJoined(GameJoinedEvent event) {
        damageSource = mc.world.getDamageSources().explosion(null);
    }

    // Crystal damage

    public static double crystalDamage(PlayerEntity player, Vec3d crystal, boolean predictMovement, BlockPos obsidianPos, boolean ignoreTerrain) {
        if (player == null) return 0;
        if (EntityUtils.getGameMode(player) == GameMode.CREATIVE && !(player instanceof FakePlayerEntity)) return 0;

        Vec3d playerPosition = predictMovement ? player.getPos().add(player.getVelocity()) : player.getPos();

        double modDistance = playerPosition.distanceTo(crystal);
        if (modDistance > 12) return 0;

        double exposure = getExposure(crystal, player, predictMovement, obsidianPos, ignoreTerrain);
        double impact = (1 - (modDistance / 12)) * exposure;
        double damage = ((impact * impact + impact) / 2 * 7 * (6 * 2) + 1);

        damage = getDamageForDifficulty(damage);
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());
        damage = resistanceReduction(player, damage);

        damage = blastProtReduction(player, damage);

        return Math.max(damage, 0);
    }

    public static double crystalDamage(PlayerEntity player, Vec3d crystal) {
        return crystalDamage(player, crystal, false, null, false);
    }

    // Sword damage

    @Deprecated(forRemoval = true)
    public static double getSwordDamage(PlayerEntity entity, boolean charged) {
        return getAttackDamage(entity);
    }

    public static double getAttackDamage(PlayerEntity entity) {
        // Get item damage
        double itemDamage = 1;
        if (entity.getActiveItem().getItem() instanceof SwordItem swordItem) itemDamage += swordItem.getAttackDamage();
        else if (entity.getActiveItem().getItem() instanceof MiningToolItem miningToolItem) itemDamage += miningToolItem.getAttackDamage();
        else if (entity.getActiveItem().getItem() instanceof ToolItem toolItem) itemDamage += toolItem.getMaterial().getAttackDamage();

        // Get enchant damage
        double enchantDamage = 0;
        if (entity.getActiveItem().getEnchantments() != null) {
            int sharpnessLevel = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, entity.getActiveItem());
            if (sharpnessLevel > 0) {
                enchantDamage = (0.5d * sharpnessLevel) + 0.5d;
            }
        }

        // Factor strength
        StatusEffectInstance strength = entity.getStatusEffect(StatusEffects.STRENGTH);
        if (strength != null) {
            itemDamage += 3 * (strength.getAmplifier() + 1);
        }

        // Factor charge
        float charge = entity.getAttackCooldownProgress(0.5f);
        itemDamage *= 0.2d + charge * charge * 0.8d;
        enchantDamage *= charge;

        // Factor critical hit
        if (charge > 0.9f && entity.fallDistance > 0f && !entity.isOnGround() && !entity.isClimbing() && !entity.isTouchingWater() && !entity.hasStatusEffect(StatusEffects.BLINDNESS) && !entity.hasVehicle()) {
            itemDamage *= 1.5d;
        }

        double damage = itemDamage + enchantDamage;

        // Factor Fire Aspect
        if (EnchantmentHelper.getFireAspect(entity) > 0 && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            damage++;
        }

        // Reduce by resistance
        damage = resistanceReduction(entity, damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft((float) damage, (float) entity.getArmor(), (float) entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by enchants
        damage = normalProtReduction(entity, damage);

        return Math.max(damage, 0);
    }

    // Bed damage

    public static double bedDamage(LivingEntity player, Vec3d bed) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).getAbilities().creativeMode) return 0;

        double modDistance = Math.sqrt(player.squaredDistanceTo(bed));
        if (modDistance > 10) return 0;

        double exposure = Explosion.getExposure(bed, player);
        double impact = (1.0 - (modDistance / 10.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (5 * 2) + 1;

        // Multiply damage by difficulty
        damage = getDamageForDifficulty(damage);

        // Reduce by resistance
        damage = resistanceReduction(player, damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by enchants
        damage = blastProtReduction(player, damage);

        return Math.max(damage, 0);
    }

    // Anchor damage

    public static double anchorDamage(LivingEntity player, Vec3d anchor) {
        BlockPos anchorPos = BlockPos.ofFloored(anchor);
        mc.world.removeBlock(anchorPos, false);
        double damage = bedDamage(player, anchor);
        mc.world.setBlockState(anchorPos, Blocks.RESPAWN_ANCHOR.getDefaultState());
        return damage;
    }

    // Utils

    private static double getDamageForDifficulty(double damage) {
        return switch (mc.world.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY     -> Math.min(damage / 2 + 1, damage);
            case HARD     -> damage * 3 / 2;
            default       -> damage;
        };
    }

    private static double normalProtReduction(Entity player, double damage) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), mc.world.getDamageSources().generic());
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel / 25.0);
        return Math.max(damage, 0);
    }

    private static double blastProtReduction(Entity player, double damage) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), damageSource);
        if (protLevel > 20) protLevel = 20;

        damage *= (1 - (protLevel / 25.0));
        return Math.max(damage, 0);
    }

    private static double resistanceReduction(LivingEntity player, double damage) {
        StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1 - (lvl * 0.2));
        }

        return Math.max(damage, 0);
    }

    private static double getExposure(Vec3d source, Entity entity, boolean predictMovement, BlockPos obsidianPos, boolean ignoreTerrain) {
        Box box = entity.getBoundingBox();
        if (predictMovement) {
            Vec3d v = entity.getVelocity();
            box = box.offset(v.x, v.y, v.z);
        }

        double xStep = 1 / ((box.maxX - box.minX) * 2 + 1);
        double yStep = 1 / ((box.maxY - box.minY) * 2 + 1);
        double zStep = 1 / ((box.maxZ - box.minZ) * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            xStep = xStep * (box.maxX - box.minX);
            yStep = yStep * (box.maxY - box.minY);
            zStep = zStep * (box.maxZ - box.minZ);

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) / 2;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) / 2;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d vec3d = new Vec3d(x, y, z);

                        if (raycast(vec3d, source, obsidianPos, ignoreTerrain) == HitResult.Type.MISS) misses++;

                        hits++;
                    }
                }
            }

            return (double) misses / hits;
        }

        return 0;
    }

    private static HitResult.Type raycast(Vec3d start, Vec3d end, BlockPos obsidianPos, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null, (_null, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(obsidianPos)) blockState = Blocks.OBSIDIAN.getDefaultState();
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) return null;
            }

            BlockHitResult hitResult = blockState.getOutlineShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (_null) -> HitResult.Type.MISS);
    }
}
