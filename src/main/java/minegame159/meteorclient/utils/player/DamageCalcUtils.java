/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

//Created by squidoodly 18/04/2020
//Updated by squidoodly 19/06/2020
//Updated by squidoodly 24/07/2020

import minegame159.meteorclient.mixininterface.IExplosion;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

import java.util.Objects;

public class DamageCalcUtils {
    private static final Explosion explosion = new Explosion(null, null, 0, 0, 0, 6, false, Explosion.DestructionType.DESTROY);
    public static MinecraftClient mc = MinecraftClient.getInstance();

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double crystalDamage(LivingEntity player, Vec3d crystal) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).abilities.creativeMode) return 0;

        // Calculate crystal damage
        double modDistance = Math.sqrt(player.squaredDistanceTo(crystal));
        if (modDistance > 12) return 0;

        double exposure = Explosion.getExposure(crystal, player);
        double impact = (1.0 - (modDistance / 12.0)) * exposure;
        double damage = (impact * impact + impact) / 2 * 7 * (6 * 2) + 1;

        // Multiply damage by difficulty
        damage = getDamageForDifficulty(damage);

        // Reduce by resistance
        damage = resistanceReduction(player, damage);

        // Reduce my armour
        damage = DamageUtil.getDamageLeft((float) damage, (float) player.getArmor(), (float) player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by enchants
        ((IExplosion) explosion).set(crystal, 6, false);
        damage = blastProtReduction(player, damage, explosion);

        return damage < 0 ? 0 : damage;
    }

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double bedDamage(LivingEntity player, Vec3d bed) {
        if (player instanceof PlayerEntity && ((PlayerEntity) player).abilities.creativeMode) return 0;

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
        ((IExplosion) explosion).set(bed, 5, true);
        damage = blastProtReduction(player, damage, explosion);

        if (damage < 0) damage = 0;
        return damage;
    }

    public static double anchorDamage(LivingEntity player, Vec3d anchor) {
        mc.world.removeBlock(new BlockPos(anchor), false);
        double damage = bedDamage(player, anchor);
        mc.world.setBlockState(new BlockPos(anchor), Blocks.RESPAWN_ANCHOR.getDefaultState());
        return damage;
    }

    public static double getSwordDamage(PlayerEntity entity, boolean charged) {
        // Get sword damage
        double damage = 0;
        if (charged) {
            if (entity.getActiveItem().getItem() == Items.DIAMOND_SWORD) {
                damage += 7;
            } else if (entity.getActiveItem().getItem() == Items.GOLDEN_SWORD) {
                damage += 4;
            } else if (entity.getActiveItem().getItem() == Items.IRON_SWORD) {
                damage += 6;
            } else if (entity.getActiveItem().getItem() == Items.STONE_SWORD) {
                damage += 5;
            } else if (entity.getActiveItem().getItem() == Items.WOODEN_SWORD) {
                damage += 4;
            }
            damage *= 1.5;
        }

        if (entity.getActiveItem().getEnchantments() != null) {
            if (EnchantmentHelper.get(entity.getActiveItem()).containsKey(Enchantments.SHARPNESS)) {
                int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, entity.getActiveItem());
                damage += (0.5 * level) + 0.5;
            }
        }

        if (entity.getActiveStatusEffects().containsKey(StatusEffects.STRENGTH)) {
            int strength = Objects.requireNonNull(entity.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            damage += 3 * strength;
        }

        // Reduce by resistance
        damage = resistanceReduction(entity, damage);

        // Reduce by armour
        damage = DamageUtil.getDamageLeft((float) damage, (float) entity.getArmor(), (float) entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        // Reduce by enchants
        damage = normalProtReduction(entity, damage);

        return damage < 0 ? 0 : damage;
    }

    private static double getDamageForDifficulty(double damage) {
        switch (mc.world.getDifficulty()) {
            case PEACEFUL: return 0;
            case EASY:     return Math.min(damage / 2.0F + 1.0F, damage);
            case HARD:     return damage * 3.0F / 2.0F;
            default:       return damage;
        }
    }

    private static double normalProtReduction(Entity player, double damage) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel / 25.0);
        return damage < 0 ? 0 : damage;
    }

    private static double blastProtReduction(Entity player, double damage, Explosion explosion) {
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if (protLevel > 20) protLevel = 20;

        damage *= 1 - (protLevel / 25.0);
        return damage < 0 ? 0 : damage;
    }

    private static double resistanceReduction(LivingEntity player, double damage) {
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= 1 - (lvl * 0.2);
        }

        return damage < 0 ? 0 : damage;
    }
}
