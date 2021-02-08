/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

//Created by squidoodly 18/04/2020
//Updated by squidoodly 19/06/2020
//Updated by squidoodly 24/07/2020

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
import net.minecraft.world.Difficulty;
import net.minecraft.world.explosion.Explosion;

import java.util.Objects;

public class DamageCalcUtils {

    public static MinecraftClient mc = MinecraftClient.getInstance();

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double crystalDamage(LivingEntity player, Vec3d crystal){
        if (player instanceof PlayerEntity && ((PlayerEntity) player).abilities.creativeMode) return 0;
        //Calculate crystal damage
        double modDistance = Math.sqrt(player.squaredDistanceTo(crystal));
        if(modDistance > 12) return 0;
        double exposure = Explosion.getExposure(crystal, player);
        double impact = (1D - (modDistance/ 12D))*exposure;
        double damage = ((impact*impact+impact) / 2 * 7 * (6 * 2) + 1);

        //Multiply damage by difficulty
        damage = getDamageMultiplied(damage);

        //Reduce by resistance
        damage = resistanceReduction(player, damage);

        //Reduce my armour
        damage = DamageUtil.getDamageLeft((float)damage, (float)player.getArmor(), (float)player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        //Reduce by enchants
        damage  = blastProtReduction(player, damage, new Explosion(mc.world, null, crystal.x, crystal.y, crystal.z, 6f, false, Explosion.DestructionType.DESTROY));


        if(damage < 0) damage = 0;
        return damage;
    }

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double bedDamage(LivingEntity player, Vec3d bed){
        if (player instanceof PlayerEntity && ((PlayerEntity) player).abilities.creativeMode) return 0;
        double modDistance = Math.sqrt(player.squaredDistanceTo(bed));
        if(modDistance > 10) return 0;
        double exposure = Explosion.getExposure(bed, player);
        double impact = (1D - (modDistance / 10D))*exposure;
        double damage = ((impact*impact+impact)/ 2 * 7 * (5 * 2) + 1);

        //Multiply damage by difficulty
        damage = getDamageMultiplied(damage);

        //Reduce by resistance
        damage = resistanceReduction(player, damage);

        //Reduce by armour
        damage = DamageUtil.getDamageLeft((float)damage, (float)player.getArmor(), (float)player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        //Reduce by enchants
        damage  = blastProtReduction(player, damage, new Explosion(mc.world, null, bed.x, bed.y, bed.z, 5f, true, Explosion.DestructionType.DESTROY));

        if (damage < 0) damage = 0;
        return damage;
    }

    public static double anchorDamage(LivingEntity player, Vec3d anchor){
        assert mc.world != null;
        mc.world.removeBlock(new BlockPos( anchor), false);
        double damage = bedDamage(player, anchor);
        mc.world.setBlockState(new BlockPos(anchor), Blocks.RESPAWN_ANCHOR.getDefaultState());
        return damage;
    }

    public static double getSwordDamage(PlayerEntity entity, boolean charged){
        //Get sword damage
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
        if(entity.getActiveItem().getEnchantments() != null){
            if(EnchantmentHelper.get(entity.getActiveItem()).containsKey(Enchantments.SHARPNESS)){
                int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, entity.getActiveItem());
                damage += (0.5 * level) + 0.5;
            }
        }
        if(entity.getActiveStatusEffects().containsKey(StatusEffects.STRENGTH)){
            int strength =  Objects.requireNonNull(entity.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            damage += 3 * strength;
        }

        //Reduce by resistance
        damage = resistanceReduction(entity, damage);

        //Reduce by armour
        damage = DamageUtil.getDamageLeft((float)damage, (float)entity.getArmor(), (float)entity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

        //Reduce by enchants
        damage = normalProtReduction(entity, damage);

        if(damage < 0) damage = 0;
        return damage;
    }

    private static double getDamageMultiplied(double damage){
        Difficulty diff = mc.world.getDifficulty();
        if (diff == Difficulty.PEACEFUL) {
            damage = 0.0F;
        }

        if (diff == Difficulty.EASY) {
            damage = Math.min(damage / 2.0F + 1.0F, damage);
        }

        if (diff == Difficulty.HARD) {
            damage = damage * 3.0F / 2.0F;
        }
        return damage;
    }

    private static double normalProtReduction(Entity player, double damage){
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if(protLevel > 20){
            protLevel = 20;
        }
        damage *= (1 - (protLevel/25d));
        if(damage < 0) damage = 0;
        return damage;
    }

    private static double blastProtReduction(Entity player, double damage, Explosion explosion){
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.explosion(explosion));
        if(protLevel > 20){
            protLevel = 20;
        }
        damage *= (1 - (protLevel/25d));
        if(damage < 0) damage = 0;
        return damage;
    }

    private static double resistanceReduction(LivingEntity player, double damage){
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            int lvl = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1);
            damage *= (1 - (lvl * 0.2));
        }
        if (damage < 0) damage = 0;
        return damage;
    }
}
