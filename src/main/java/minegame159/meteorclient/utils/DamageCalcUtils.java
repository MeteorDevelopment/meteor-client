package minegame159.meteorclient.utils;

//Created by squidoodly 18/04/2020

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;

import java.util.Iterator;

public class DamageCalcUtils {

    public static MinecraftClient mc = MinecraftClient.getInstance();

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double crystalDamage(Entity player, Entity crystal){
        boolean feetExposed = mc.world.rayTrace(
                new RayTraceContext(player.getPos(), crystal.getPos(),
                        RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, player)).getType()
                == HitResult.Type.MISS;
        boolean headExposed = mc.world.rayTrace(
                new RayTraceContext(player.getPos().add(0, 1, 0), crystal.getPos(),
                        RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, player)).getType()
                == HitResult.Type.MISS;
        double exposure = 0D;
        if(feetExposed && headExposed){
            exposure = 1D;
        }else if(feetExposed ^ headExposed){
            exposure = 0.5D;
        }
        double impact = (1D - mc.player.distanceTo(crystal) / 12D)*exposure;
        return (impact*impact+impact)*42+1;
    }

    //Always Calculate damage, then armour, then enchantments, then potion effect
    public static double bedDamage(Entity player, BlockEntity bed){
        boolean feetExposed = mc.world.rayTrace(
                new RayTraceContext(player.getPos(), toVec3D(bed),
                        RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, player)).getType()
                == HitResult.Type.MISS;
        boolean headExposed = mc.world.rayTrace(
                new RayTraceContext(player.getPos().add(0, 1, 0), toVec3D(bed),
                        RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, player)).getType()
                == HitResult.Type.MISS;
        double exposure = 0D;
        if(feetExposed && headExposed){
            exposure = 1D;
        }else if(feetExposed ^ headExposed){
            exposure = 0.5D;
        }
        double impact = (1D - distanceBetween(player, bed) / 12D)*exposure;
        return (impact*impact+impact)*42+1;
    }

    public static double armourCalc(Entity player ,double damage){
        double defencePoints = 0;
        double toughness = 0;
        Iterator<ItemStack> playerArmour = player.getArmorItems().iterator();
        Iterator<ItemStack> playerArmourEnchants = player.getArmorItems().iterator();
        Item boots = playerArmour.next().getItem();
        Item leggings = playerArmour.next().getItem();
        Item chestplate = playerArmour.next().getItem();
        Item helmet = playerArmour.next().getItem();
        if( boots == Items.DIAMOND_BOOTS){
            defencePoints += 3;
            toughness += 2;
        }else if(boots == Items.IRON_BOOTS){
            defencePoints += 2;
        }else if(boots == Items.LEATHER_BOOTS){
            defencePoints += 1;
        }else if(boots == Items.GOLDEN_BOOTS){
            defencePoints += 1;
        }else if(boots == Items.CHAINMAIL_BOOTS){
            defencePoints += 1;
        }
        if( leggings == Items.DIAMOND_LEGGINGS){
            defencePoints += 6;
            toughness += 2;
        }else if(leggings == Items.IRON_LEGGINGS){
            defencePoints += 5;
        }else if(leggings == Items.LEATHER_BOOTS){
            defencePoints += 2;
        }else if(leggings == Items.GOLDEN_BOOTS){
            defencePoints += 3;
        }else if(leggings == Items.CHAINMAIL_BOOTS){
            defencePoints += 4;
        }
        if( chestplate == Items.DIAMOND_CHESTPLATE){
            defencePoints += 8;
            toughness += 2;
        }else if(chestplate == Items.IRON_CHESTPLATE){
            defencePoints += 6;
        }else if(chestplate == Items.LEATHER_CHESTPLATE){
            defencePoints += 3;
        }else if(chestplate == Items.GOLDEN_CHESTPLATE){
            defencePoints += 5;
        }else if(chestplate == Items.CHAINMAIL_CHESTPLATE){
            defencePoints += 5;
        }
        if( helmet == Items.DIAMOND_HELMET){
            defencePoints += 3;
            toughness += 2;
        }else if(helmet == Items.IRON_HELMET){
            defencePoints += 2;
        }else if(helmet == Items.LEATHER_HELMET){
            defencePoints += 1;
        }else if(helmet == Items.GOLDEN_HELMET){
            defencePoints += 2;
        }else if(helmet == Items.CHAINMAIL_HELMET){
            defencePoints += 2;
        }else if(helmet == Items.TURTLE_HELMET){
            defencePoints += 2;
        }
        damage = damage*(1 - ((Math.min(20, Math.max((defencePoints/5), defencePoints - (damage/(2+(toughness/4))))))/25));
        return damage;
    }

    public static double blastProtReduction(Entity player, double damage){
        int protLevel = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), DamageSource.FIREWORKS);
        if(protLevel > 20){
            protLevel = 20;
        }
        damage = damage * (1-(protLevel/25));
        return damage;
    }

    public static double resistanceReduction(double damage){
        int level = 0;
        if(mc.player.getStatusEffects().equals(StatusEffects.RESISTANCE)){
            level = mc.player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier();

        }
        damage = damage * (1 - (0.2 * level));
        return damage;
    }

    private static Vec3d toVec3D(BlockEntity bed){
        return new Vec3d(bed.getPos().getX(), bed.getPos().getY(), bed.getPos().getZ());
    }

    private static double distanceBetween(Entity player, BlockEntity bed){
        return (Math.abs(player.getPos().getX() - bed.getPos().getX()) + Math.abs(player.getPos().getY() - bed.getPos().getY()) + Math.abs(player.getPos().getZ() - bed.getPos().getZ()));
    }

}
