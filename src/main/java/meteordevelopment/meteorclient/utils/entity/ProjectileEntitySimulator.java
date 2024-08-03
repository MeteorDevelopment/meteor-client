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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProjectileEntitySimulator {
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private static final Vec3d pos3d = new Vec3d(0, 0, 0);
    private static final Vec3d prevPos3d = new Vec3d(0, 0, 0);

    public final Vector3d pos = new Vector3d();
    private final Vector3d velocity = new Vector3d();

    private Entity simulatingEntity;
    private double gravity;
    private double airDrag, waterDrag;
    private float height, width;


    // held items

    public boolean set(Entity user, ItemStack itemStack, double simulated, boolean accurate, double tickDelta) {
        Item item = itemStack.getItem();

        switch (item) {
            case BowItem ignored -> {
                double charge = BowItem.getPullProgress(mc.player.getItemUseTime());
                if (charge <= 0.1) return false;

                set(user, 0, charge * 3, simulated, 0.05, 0.6, accurate, tickDelta, EntityType.ARROW);
            }
            case CrossbowItem ignored -> {
                ChargedProjectilesComponent projectilesComponent = itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (projectilesComponent == null) return false;

                if (projectilesComponent.contains(Items.FIREWORK_ROCKET)) {
                    set(user, 0, CrossbowItemAccessor.getSpeed(projectilesComponent), simulated, 0, 0.6, accurate, tickDelta, EntityType.FIREWORK_ROCKET);
                }
                else set(user, 0, CrossbowItemAccessor.getSpeed(projectilesComponent), simulated, 0.05, 0.6, accurate, tickDelta, EntityType.ARROW);
            }
            case WindChargeItem ignored -> {
                set(user, 0, 1.5, simulated, 0, 1.0, accurate, tickDelta, EntityType.WIND_CHARGE);
                this.airDrag = 1.0;
            }
            case FishingRodItem ignored         -> setFishingBobber(user, tickDelta);
            case TridentItem ignored            -> set(user, 0, 2.5, simulated, 0.05, 0.99, accurate, tickDelta, EntityType.TRIDENT);
            case SnowballItem ignored           -> set(user, 0, 1.5, simulated, 0.03, 0.8, accurate, tickDelta, EntityType.SNOWBALL);
            case EggItem ignored                -> set(user, 0, 1.5, simulated, 0.03, 0.8, accurate, tickDelta, EntityType.EGG);
            case EnderPearlItem ignored         -> set(user, 0, 1.5, simulated, 0.03, 0.8, accurate, tickDelta, EntityType.ENDER_PEARL);
            case ExperienceBottleItem ignored   -> set(user, -20, 0.7, simulated, 0.07, 0.8, accurate, tickDelta, EntityType.EXPERIENCE_BOTTLE);
            case ThrowablePotionItem ignored    -> set(user, -20, 0.5, simulated, 0.05, 0.8, accurate, tickDelta, EntityType.POTION);
            default -> {
                return false;
            }
        }

        return true;
    }

    public void set(Entity user, double roll, double speed, double simulated, double gravity, double waterDrag, boolean accurate, double tickDelta, EntityType<?> type) {
        Utils.set(pos, user, tickDelta).add(0, user.getEyeHeight(user.getPose()), 0);

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
            Quaterniond quaternion = new Quaterniond().setAngleAxis(simulated, vec3d.x, vec3d.y, vec3d.z);
            Vec3d vec3d2 = user.getRotationVec(1.0F);
            Vector3d vector3f = new Vector3d(vec3d2.x, vec3d2.y, vec3d2.z);
            vector3f.rotate(quaternion);

            x = vector3f.x;
            y = vector3f.y;
            z = vector3f.z;
        }

        velocity.set(x, y, z).normalize().mul(speed);

        if (accurate) {
            Vec3d vel = user.getVelocity();
            velocity.add(vel.x, user.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.simulatingEntity = user;
        this.gravity = gravity;
        this.airDrag = 0.99;
        this.waterDrag = waterDrag;
        this.width = type.getWidth();
        this.height = type.getHeight();
    }


    // fired projectiles

    public boolean set(Entity entity, boolean accurate) {
        // skip entities in ground
        if (entity instanceof ProjectileInGroundAccessor ppe && ppe.getInGround()) return false;

        if (entity instanceof ArrowEntity) {
            set(entity, 0.05, 0.6, accurate);
        } else if (entity instanceof TridentEntity) {
            set(entity, 0.05, 0.99, accurate);
        }

        else if (entity instanceof EnderPearlEntity || entity instanceof SnowballEntity || entity instanceof EggEntity) {
            set(entity, 0.03, 0.8, accurate);
        } else if (entity instanceof ExperienceBottleEntity) {
            set(entity,  0.07, 0.8, accurate);
        } else if (entity instanceof PotionEntity) {
            set(entity, 0.05, 0.8, accurate);
        }

        else if (entity instanceof WitherSkullEntity || entity instanceof FireballEntity || entity instanceof DragonFireballEntity || entity instanceof WindChargeEntity) {
            // drag isn't actually 1, but this provides accurate results in 99.9% in of real situations.
            set(entity, 0, 1.0, accurate);
            this.airDrag = 1.0;
        }
        else {
            return false;
        }

        if (entity.hasNoGravity()) {
            this.gravity = 0;
        }

        return true;
    }

    public void set(Entity entity, double gravity, double waterDrag, boolean accurate) {
        pos.set(entity.getX(), entity.getY(), entity.getZ());

        double speed = entity.getVelocity().length();
        velocity.set(entity.getVelocity().x, entity.getVelocity().y, entity.getVelocity().z).normalize().mul(speed);

        if (accurate) {
            Vec3d vel = entity.getVelocity();
            velocity.add(vel.x, entity.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.simulatingEntity = entity;
        this.gravity = gravity;
        this.airDrag = 0.99;
        this.waterDrag = waterDrag;
        this.width = entity.getWidth();
        this.height = entity.getHeight();
    }

    public void setFishingBobber(Entity user, double tickDelta) {
        double yaw = MathHelper.lerp(tickDelta, user.prevYaw, user.getYaw());
        double pitch = MathHelper.lerp(tickDelta, user.prevPitch, user.getPitch());

        double h = Math.cos(-yaw * 0.017453292F - 3.1415927F);
        double i = Math.sin(-yaw * 0.017453292F - 3.1415927F);
        double j = -Math.cos(-pitch * 0.017453292F);
        double k = Math.sin(-pitch * 0.017453292F);

        Utils.set(pos, user, tickDelta).sub(i * 0.3, 0, h * 0.3).add(0, user.getEyeHeight(user.getPose()), 0);

        velocity.set(-i, MathHelper.clamp(-(k / j), -5, 5), -h);

        double l = velocity.length();
        velocity.mul(0.6 / l + 0.5, 0.6 / l + 0.5, 0.6 / l + 0.5);

        simulatingEntity = user;
        gravity = 0.03;
        airDrag = 0.92;
        waterDrag = 0;
        width = EntityType.FISHING_BOBBER.getWidth();
        height = EntityType.FISHING_BOBBER.getHeight();
    }

    public HitResult tick() {
        // Apply velocity
        ((IVec3d) prevPos3d).set(pos);
        pos.add(velocity);

        // Update velocity
        velocity.mul(isTouchingWater() ? waterDrag : airDrag);
        velocity.sub(0, gravity, 0);

        // Check if below world
        if (pos.y < mc.world.getBottomY()) return MissHitResult.INSTANCE;

        // Check if chunk is loaded
        int chunkX = ChunkSectionPos.getSectionCoord(pos.x);
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.z);
        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return MissHitResult.INSTANCE;

        // Check for collision
        ((IVec3d) pos3d).set(pos);
        if (pos3d.equals(prevPos3d)) return MissHitResult.INSTANCE;

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
        HitResult hitResult = mc.world.raycast(new RaycastContext(prevPos3d, pos3d, RaycastContext.ShapeType.COLLIDER, waterDrag == 0 ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, simulatingEntity));
        if (hitResult.getType() != HitResult.Type.MISS) {
            ((IVec3d) pos3d).set(hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z);
        }

        // Vanilla uses the current and next positions to check collisions, we use the previous and current positions
        Box box = new Box(prevPos3d.x - (width / 2f), prevPos3d.y, prevPos3d.z - (width / 2f), prevPos3d.x + (width / 2f), prevPos3d.y + height, prevPos3d.z + (width / 2f))
            .stretch(velocity.x, velocity.y, velocity.z).expand(1.0D);
        HitResult hitResult2 = ProjectileUtil.getEntityCollision(
            mc.world, simulatingEntity == mc.player ? null : simulatingEntity, prevPos3d, pos3d, box, entity -> !entity.isSpectator() && entity.isAlive() && entity.canHit()
        );
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }
}
