package minegame159.meteorclient.utils;

//Created by squidoodly 18/04/2020

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;

public class DamageCalcUtils {

    public static MinecraftClient mc = MinecraftClient.getInstance();


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

    private static Vec3d toVec3D(BlockEntity bed){
        return new Vec3d(bed.getPos().getX(), bed.getPos().getY(), bed.getPos().getZ());
    }

    private static double distanceBetween(Entity player, BlockEntity bed){
        return (Math.abs(player.getPos().getX() - bed.getPos().getX()) + Math.abs(player.getPos().getY() - bed.getPos().getY()) + Math.abs(player.getPos().getZ() - bed.getPos().getZ()));
    }

}
