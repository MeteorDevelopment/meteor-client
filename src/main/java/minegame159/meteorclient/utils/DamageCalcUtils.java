package minegame159.meteorclient.utils;

//Created by squidoodly 18/04/2020
//Updated by squidoodly 19/06/2020

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

import java.util.Iterator;
import java.util.Objects;

public class DamageCalcUtils {

    public static MinecraftClient mc = MinecraftClient.getInstance();

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double crystalDamage(Entity player, Vec3d crystal){
        if(Math.sqrt(mc.player.squaredDistanceTo(crystal)) > 12) return 0;
        double exposure = Explosion.getExposure(crystal, player);
        double impact = (1D - (Math.sqrt(mc.player.squaredDistanceTo(crystal)) / 12D))*exposure;
        return (impact*impact+impact)*42+1;
    }

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double bedDamage(Entity player, Vec3d bed){
        if(Math.sqrt(mc.player.squaredDistanceTo(bed)) > 10) return 0;
        double exposure = Explosion.getExposure(bed, player);
        double impact = (1D - (Math.sqrt(mc.player.squaredDistanceTo(bed)) / 10D))*exposure;
        return (impact*impact+impact)*35+1;
    }

    public static double armourCalc(Entity player, double damage){
        double defencePoints = 0;
        float toughness = 0;
        Iterator<ItemStack> playerArmour = player.getArmorItems().iterator();
        Item boots = playerArmour.next().getItem();
        Item leggings = playerArmour.next().getItem();
        Item chestplate = playerArmour.next().getItem();
        Item helmet = playerArmour.next().getItem();
        if(boots instanceof ArmorItem){
            defencePoints += getDefencePoints((ArmorItem) boots);
            toughness += getArmourToughness((ArmorItem) boots);
        }
        if(leggings instanceof ArmorItem){
            defencePoints += getDefencePoints((ArmorItem) leggings);
            toughness += getArmourToughness((ArmorItem) leggings);
        }
        if(chestplate instanceof ArmorItem){
            defencePoints += getDefencePoints((ArmorItem) chestplate);
            toughness = toughness + getArmourToughness((ArmorItem) chestplate);
        }
        if(helmet instanceof ArmorItem){
            defencePoints += getDefencePoints((ArmorItem) helmet);
            toughness += getArmourToughness((ArmorItem) helmet);
        }
        damage = damage*(1 - ((Math.min(20, Math.max((defencePoints/5), defencePoints - (damage/(2+(toughness/4))))))/25));
        if(damage < 0) damage = 0;
        return damage;
    }

    public static double getSwordDamage(PlayerEntity entity){
        float damage = 0;
        if(entity.getActiveItem().getItem() == Items.DIAMOND_SWORD) {
            damage += 7;
        }else if(entity.getActiveItem().getItem() == Items.GOLDEN_SWORD){
            damage += 4;
        }else if(entity.getActiveItem().getItem() == Items.IRON_SWORD){
            damage += 6;
        }else if(entity.getActiveItem().getItem() == Items.STONE_SWORD){
            damage += 5;
        }else if(entity.getActiveItem().getItem() == Items.WOODEN_SWORD){
            damage += 4;
        }
        damage *= 1.5;
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
        return damage;
    }

    public static int getDefencePoints(ArmorItem item){
        return item.getProtection();
    }

    public static float getArmourToughness(ArmorItem item){
        return item.getMaterial().getToughness();
    }

    public static double getDamageMultiplied(double damage){
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0 : (diff == 1 ? 0.5f : (diff == 2 ? 1 : 1.5f)));
    }

    public static double normalProtReduction(Entity player, double damage){
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if(protLevel > 20){
            protLevel = 20;
        }
        damage *= (1 - (protLevel/25d));
        if(damage < 0) damage = 0;
        return damage;
    }

    public static double blastProtReduction(Entity player, double damage){
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.GENERIC);
        if(protLevel > 20){
            protLevel = 20;
        }
        damage *= (1 - (protLevel/25d));
        if(damage < 0) damage = 0;
        return damage;
    }

    public static double resistanceReduction(PlayerEntity player, double damage){
        int level = 0;
        if(player.getActiveStatusEffects().containsKey(StatusEffects.RESISTANCE)){
            level = Objects.requireNonNull(player.getStatusEffect(StatusEffects.RESISTANCE)).getAmplifier() + 1;
        }
        damage = damage * (1 - (0.2 * level));
        if(damage < 0) damage = 0;
        return damage;
    }

    private static Vec3d toVec3D(BlockEntity bed){
        return new Vec3d(bed.getPos().getX(), bed.getPos().getY(), bed.getPos().getZ());
    }

    private static double distanceBetween(Entity player, BlockEntity bed){
        return (Math.abs(player.getPos().getX() - bed.getPos().getX()) + Math.abs(player.getPos().getY() - bed.getPos().getY()) + Math.abs(player.getPos().getZ() - bed.getPos().getZ()));
    }

}
