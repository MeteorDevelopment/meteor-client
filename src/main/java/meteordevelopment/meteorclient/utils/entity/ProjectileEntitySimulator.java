/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.mixin.CrossbowItemAccessor;
import meteordevelopment.meteorclient.mixin.ProjectileInGroundAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MissHitResult;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProjectileEntitySimulator {
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private static final Vec3d pos3d = new Vec3d(0, 0, 0);
    private static final Vec3d prevPos3d = new Vec3d(0, 0, 0);

    public final Vec3 pos = new Vec3();
    private final Vec3 velocity = new Vec3();

    private double gravity;
    private double airDrag, waterDrag;

    public boolean set(Entity user, ItemStack itemStack, double simulated, boolean accurate, double tickDelta) {
        Item item = itemStack.getItem();

        if (item instanceof BowItem) {
            double charge = BowItem.getPullProgress(mc.player.getItemUseTime());
            if (charge <= 0) return false;

            set(user, 0, charge * 3, simulated, 0.05000000074505806, 0.6, accurate, tickDelta);
        }
        else if (item instanceof CrossbowItem) {
            if (!CrossbowItem.isCharged(itemStack)) return false;

            set(user, 0, CrossbowItemAccessor.getSpeed(itemStack), simulated, 0.05000000074505806, 0.6, accurate, tickDelta);
        }
        else if (item instanceof FishingRodItem) {
            setFishingBobber(user, tickDelta);
        }
        else if (item instanceof TridentItem) {
            set(user, 0, 2.5, simulated, 0.05000000074505806, 0.99, accurate, tickDelta);
        }
        else if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) {
            set(user, 0, 1.5, simulated, 0.03, 0.8, accurate, tickDelta);
        }
        else if (item instanceof ExperienceBottleItem) {
            set(user, -20, 0.7, simulated, 0.07, 0.8, accurate, tickDelta);
        }
        else if (item instanceof ThrowablePotionItem) {
            set(user, -20, 0.5, simulated, 0.05, 0.8, accurate, tickDelta);
        }
        else {
            return false;
        }

        return true;
    }

    public void set(Entity user, double roll, double speed, double simulated, double gravity, double waterDrag, boolean accurate, double tickDelta) {
        pos.set(user, tickDelta).add(0, user.getEyeHeight(user.getPose()), 0);

        double yaw = MathHelper.lerp(tickDelta, user.prevYaw, user.getYaw());
        double pitch = MathHelper.lerp(tickDelta, user.prevPitch, user.getPitch());

        double x, y, z;

        if (simulated == 0) {
            x = -Math.sin(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
            y = -Math.sin((pitch + roll) * 0.017453292);
            z = Math.cos(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
        }
        else {
            Vec3d vec3d = user.getOppositeRotationVector(1.0F);
            Quaternion quaternion = new Quaternion(new Vec3f(vec3d), (float) simulated, true);
            Vec3d vec3d2 = user.getRotationVec(1.0F);
            Vec3f vector3f = new Vec3f(vec3d2);
            vector3f.rotate(quaternion);

            x = vector3f.getX();
            y = vector3f.getY();
            z = vector3f.getZ();
        }

        velocity.set(x, y, z).normalize().multiply(speed);

        if (accurate) {
            Vec3d vel = user.getVelocity();
            velocity.add(vel.x, user.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.gravity = gravity;
        this.airDrag = 0.99;
        this.waterDrag = waterDrag;
    }

    public boolean set(Entity entity, boolean accurate, double tickDelta) {
        // skip entities in ground
        if (entity instanceof PersistentProjectileEntity && ((ProjectileInGroundAccessor) entity).getInGround()) return false;

        if (entity instanceof ArrowEntity arrow) {
            // im not sure if arrow.getVelocity().length() is correct but it works ¯\_(ツ)_/¯
            set(entity, arrow.getVelocity().length(), 0.05000000074505806, 0.6, accurate, tickDelta);
        } else if (entity instanceof EnderPearlEntity || entity instanceof SnowballEntity || entity instanceof EggEntity) {
            set(entity, 1.5, 0.03, 0.8, accurate, tickDelta);
        } else if (entity instanceof TridentEntity) {
            set(entity, 2.5, 0.05000000074505806, 0.99, accurate, tickDelta);
        } else if (entity instanceof ExperienceBottleEntity) {
            set(entity, 0.7,  0.07, 0.8, accurate, tickDelta);
        } else if (entity instanceof ThrownEntity) {
            set(entity, 0.5, 0.05, 0.8, accurate, tickDelta);
        } else if (entity instanceof WitherSkullEntity || entity instanceof FireballEntity || entity instanceof DragonFireballEntity) {
            set(entity, 0.95, 0, 0.8, accurate, tickDelta);
        }
        else {
            return false;
        }

        return true;
    }

    public void set(Entity entity, double speed, double gravity, double waterDrag, boolean accurate, double tickDelta) {
        pos.set(entity, tickDelta);

        velocity.set(entity.getVelocity()).normalize().multiply(speed);

        if (accurate) {
            Vec3d vel = entity.getVelocity();
            velocity.add(vel.x, entity.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.gravity = gravity;
        this.airDrag = 0.99;
        this.waterDrag = waterDrag;
    }

    public void setFishingBobber(Entity user, double tickDelta) {
        double yaw = MathHelper.lerp(tickDelta, user.prevYaw, user.getYaw());
        double pitch = MathHelper.lerp(tickDelta, user.prevPitch, user.getPitch());

        double h = Math.cos(-yaw * 0.017453292F - 3.1415927F);
        double i = Math.sin(-yaw * 0.017453292F - 3.1415927F);
        double j = -Math.cos(-pitch * 0.017453292F);
        double k = Math.sin(-pitch * 0.017453292F);

        pos.set(user, tickDelta).subtract(i * 0.3, 0, h * 0.3).add(0, user.getEyeHeight(user.getPose()), 0);

        velocity.set(-i, Utils.clamp(-(k / j), -5, 5), -h);

        double l = velocity.length();
        velocity.multiply(0.6 / l + 0.5, 0.6 / l + 0.5, 0.6 / l + 0.5);

        gravity = 0.03;
        airDrag = 0.92;
        waterDrag = 0;
    }

    public HitResult tick() {
        // Apply velocity
        ((IVec3d) prevPos3d).set(pos);
        pos.add(velocity);

        // Update velocity
        velocity.multiply(isTouchingWater() ? waterDrag : airDrag);
        velocity.subtract(0, gravity, 0);

        // Check if below world
        if (pos.y < mc.world.getBottomY()) return MissHitResult.INSTANCE;

        // Check if chunk is loaded
        int chunkX = (int) (pos.x / 16);
        int chunkZ = (int) (pos.z / 16);
        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return MissHitResult.INSTANCE;

        // Check for collision
        ((IVec3d) pos3d).set(pos);
        HitResult hitResult = getCollision();

        return hitResult.getType() == HitResult.Type.MISS ? null : hitResult;
    }

    private boolean isTouchingWater() {
        blockPos.set(pos.x, pos.y, pos.z);

        FluidState fluidState = mc.world.getFluidState(blockPos);
        if (fluidState.getFluid() != Fluids.WATER && fluidState.getFluid() != Fluids.FLOWING_WATER) return false;

        return pos.y - (int) pos.y <= fluidState.getHeight();
    }

    private HitResult getCollision() {
        Vec3d vec3d3 = prevPos3d;

        HitResult hitResult = mc.world.raycast(new RaycastContext(vec3d3, pos3d, RaycastContext.ShapeType.COLLIDER, waterDrag == 0 ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, mc.player));
        if (hitResult.getType() != HitResult.Type.MISS) {
            vec3d3 = hitResult.getPos();
        }

        HitResult hitResult2 = ProjectileUtil.getEntityCollision(mc.world, mc.player, vec3d3, pos3d, new Box(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).stretch(mc.player.getVelocity()).expand(1.0D), entity -> !entity.isSpectator() && entity.isAlive() && entity.canHit());
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }
}
