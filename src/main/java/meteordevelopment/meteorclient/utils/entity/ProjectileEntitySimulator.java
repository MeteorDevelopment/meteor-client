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
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProjectileEntitySimulator {
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Vec3d pos3d = new Vec3d(0, 0, 0);
    private final Vec3d prevPos3d = new Vec3d(0, 0, 0);

    public final Vector3d pos = new Vector3d();
    private final Vector3d velocity = new Vector3d();

    private Entity simulatingEntity;
    private EntityDimensions dimensions;
    private double gravity;
    private double airDrag, waterDrag;

    public record MotionData(float power, float roll, double gravity, float airDrag, float waterDrag, EntityType<?> entity) {
        public MotionData withPower(float power) {
            return new MotionData(power, this.roll(), this.gravity(), this.airDrag(), this.waterDrag(), this.entity());
        }
    }

    // ThrownEntity
    private static final MotionData EGG                = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.EGG);
    private static final MotionData ENDER_PEARL        = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.ENDER_PEARL);
    private static final MotionData SNOWBALL           = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.SNOWBALL);
    private static final MotionData EXPERIENCE_BOTTLE  = new MotionData(0.7f, -20, 0.07, 0.99f, 0.8f, EntityType.EXPERIENCE_BOTTLE);
    private static final MotionData LINGERING_POTION   = new MotionData(0.5f, -20, 0.05, 0.99f, 0.8f, EntityType.LINGERING_POTION);
    private static final MotionData SPLASH_POTION      = new MotionData(0.5f, -20, 0.05, 0.99f, 0.8f, EntityType.SPLASH_POTION);

    // ExplosiveProjectileEntity
    private static final MotionData EXPLOSIVE          = new MotionData(0, 0, 0, 1, 1, null); // fireball, wither skull, etc.
    private static final MotionData WIND_CHARGE        = new MotionData(1.5f, 0, 0, 1, 1, EntityType.WIND_CHARGE);

    // PersistentProjectileEntity
    private static final MotionData ARROW              = new MotionData(0, 0, 0.05, 0.99f, 0.6f, EntityType.ARROW);
    private static final MotionData TRIDENT            = new MotionData(2.5f, 0, 0.05, 0.99f, 0.99f, EntityType.TRIDENT);

    // Other
    private static final MotionData FIREWORK_ROCKET    = new MotionData(0, 0, 0, 1, 1, EntityType.FIREWORK_ROCKET);
    private static final MotionData FISHING_BOBBER     = new MotionData(0, 0, 0.03, 0.92f, 0, EntityType.FISHING_BOBBER);
    private static final MotionData LLAMA_SPIT         = new MotionData(1.5f, 0, 0.06, 0.99f, 0, EntityType.LLAMA_SPIT);

    // held items

    public boolean set(Entity user, ItemStack itemStack, double angleOffset, boolean accurate, float tickDelta) {
        Item item = itemStack.getItem();

        switch (item) {
            case BowItem ignored -> {
                float charge = BowItem.getPullProgress(mc.player.getItemUseTime());
                if (charge <= 0.1) return false;

                set(user, angleOffset, accurate, tickDelta, ARROW.withPower(charge * 3));
            }
            case CrossbowItem ignored -> {
                ChargedProjectilesComponent projectilesComponent = itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                if (projectilesComponent == null) return false;

                float speed = CrossbowItemAccessor.meteor$getSpeed(projectilesComponent);
                if (projectilesComponent.contains(Items.FIREWORK_ROCKET)) {
                    set(user, angleOffset, accurate, tickDelta, FIREWORK_ROCKET.withPower(speed));
                }
                else set(user, angleOffset,accurate, tickDelta, ARROW.withPower(speed));
            }
            case WindChargeItem ignored         -> set(user, angleOffset, accurate, tickDelta, WIND_CHARGE);
            case TridentItem ignored            -> set(user, angleOffset, accurate, tickDelta, TRIDENT);
            case SnowballItem ignored           -> set(user, angleOffset, accurate, tickDelta, SNOWBALL);
            case EggItem ignored                -> set(user, angleOffset, accurate, tickDelta, EGG);
            case EnderPearlItem ignored         -> set(user, angleOffset, accurate, tickDelta, ENDER_PEARL);
            case ExperienceBottleItem ignored   -> set(user, angleOffset, accurate, tickDelta, EXPERIENCE_BOTTLE);
            case SplashPotionItem ignored       -> set(user, angleOffset, accurate, tickDelta, SPLASH_POTION);
            case LingeringPotionItem ignored    -> set(user, angleOffset, accurate, tickDelta, LINGERING_POTION);
            case FishingRodItem ignored         -> setFishingBobber(user, tickDelta, FISHING_BOBBER);
            default -> {
                return false;
            }
        }

        return true;
    }

    public void set(Entity user, double angleOffset, boolean accurate, float tickDelta, MotionData data) {
        Utils.set(pos, user, tickDelta).add(0, user.getEyeHeight(user.getPose()), 0);

        double yaw;
        double pitch;

        if (user == mc.player && Rotations.rotating) {
            yaw = Rotations.serverYaw;
            pitch = Rotations.serverPitch;
        } else {
            yaw = user.getYaw(tickDelta);
            pitch = user.getPitch(tickDelta);
        }

        double x, y, z;

        if (angleOffset == 0) {
            x = -Math.sin(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
            y = -Math.sin((pitch + data.roll()) * 0.017453292);
            z = Math.cos(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
        }
        else {
            Vec3d vec3d = user.getOppositeRotationVector(1.0F);
            Quaterniond quaternion = new Quaterniond().setAngleAxis(angleOffset, vec3d.x, vec3d.y, vec3d.z);
            Vec3d vec3d2 = user.getRotationVec(1.0F);
            Vector3d vector3f = new Vector3d(vec3d2.x, vec3d2.y, vec3d2.z);
            vector3f.rotate(quaternion);

            x = vector3f.x;
            y = vector3f.y;
            z = vector3f.z;
        }

         velocity.set(x, y, z).normalize().mul(data.power());

        if (accurate) {
            Vec3d vel = user.getVelocity();
            velocity.add(vel.x, user.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.gravity = data.gravity();
        this.airDrag = data.airDrag();
        this.waterDrag = data.waterDrag();
        this.simulatingEntity = data.entity().create(mc.world, null);
        this.dimensions = simulatingEntity.getDimensions(simulatingEntity.getPose());
    }


    // fired projectiles

    public boolean set(Entity entity, boolean accurate) {
        // skip entities in ground
        if (entity instanceof ProjectileInGroundAccessor ppe && ppe.meteor$invokeIsInGround()) return false;

        switch (entity) {
            case ArrowEntity e                  -> set(e, accurate, ARROW);
            case SpectralArrowEntity e          -> set(e, accurate, ARROW);
            case TridentEntity e                -> set(e, accurate, TRIDENT);
            case EnderPearlEntity e             -> set(e, accurate, ENDER_PEARL);
            case SnowballEntity e               -> set(e, accurate, SNOWBALL);
            case EggEntity e                    -> set(e, accurate, EGG);
            case ExperienceBottleEntity e       -> set(e, accurate, EXPERIENCE_BOTTLE);
            case SplashPotionEntity e           -> set(e, accurate, SPLASH_POTION);
            case LingeringPotionEntity e        -> set(e, accurate, LINGERING_POTION);
            case AbstractWindChargeEntity e     -> set(e, accurate, WIND_CHARGE);
            case ExplosiveProjectileEntity e    -> set(e, accurate, EXPLOSIVE);
            case LlamaSpitEntity e              -> set(e, accurate, LLAMA_SPIT);
            default -> {
                return false;
            }
        }

        if (entity.hasNoGravity()) {
            this.gravity = 0;
        }

        return true;
    }

    public void set(Entity entity, boolean accurate, MotionData data) {
        pos.set(entity.getX(), entity.getY(), entity.getZ());

        double speed = entity.getVelocity().length();
        velocity.set(entity.getVelocity().x, entity.getVelocity().y, entity.getVelocity().z).normalize().mul(speed);

        if (accurate) {
            Vec3d vel = entity.getVelocity();
            velocity.add(vel.x, entity.isOnGround() ? 0.0D : vel.y, vel.z);
        }

        this.gravity = data.gravity();
        this.airDrag = data.airDrag();
        this.waterDrag = data.waterDrag();
        this.simulatingEntity = entity;
        this.dimensions = simulatingEntity.getDimensions(simulatingEntity.getPose());
    }

    public void setFishingBobber(Entity user, float tickDelta, MotionData data) {
        double yaw;
        double pitch;

        if (user == mc.player && Rotations.rotating) {
            yaw = Rotations.serverYaw;
            pitch = Rotations.serverPitch;
        } else {
            yaw = user.getYaw(tickDelta);
            pitch = user.getPitch(tickDelta);
        }

        double h = Math.cos(-yaw * 0.017453292F - 3.1415927F);
        double i = Math.sin(-yaw * 0.017453292F - 3.1415927F);
        double j = -Math.cos(-pitch * 0.017453292F);
        double k = Math.sin(-pitch * 0.017453292F);

        Utils.set(pos, user, tickDelta).sub(i * 0.3, 0, h * 0.3).add(0, user.getEyeHeight(user.getPose()), 0);

        velocity.set(-i, MathHelper.clamp(-(k / j), -5, 5), -h);

        double l = velocity.length();
        velocity.mul(0.6 / l + 0.5, 0.6 / l + 0.5, 0.6 / l + 0.5);

        this.simulatingEntity = data.entity().create(mc.world, null);
        this.dimensions = simulatingEntity.getDimensions(simulatingEntity.getPose());
        this.gravity = data.gravity();
        this.airDrag = data.airDrag();
        this.waterDrag = data.waterDrag();
    }

    // https://minecraft.wiki/w/Projectile
    // https://minecraft.wiki/w/Entity#Motion
    public HitResult tick() {
        ((IVec3d) prevPos3d).meteor$set(pos);

        // gravity -> drag -> position
        if (simulatingEntity instanceof ThrownEntity || simulatingEntity instanceof ExplosiveProjectileEntity) {
            velocity.sub(0, gravity, 0);
            velocity.mul(isTouchingWater() ? waterDrag : airDrag);
            pos.add(velocity);
        }
        // position -> drag -> gravity
        else if (simulatingEntity instanceof PersistentProjectileEntity || simulatingEntity instanceof LlamaSpitEntity) {
            pos.add(velocity);
            velocity.mul(isTouchingWater() ? waterDrag : airDrag);
            velocity.sub(0, gravity, 0);
        }
        // gravity -> position > drag
        else if (simulatingEntity instanceof FishingBobberEntity) {
            velocity.sub(0, gravity, 0);
            pos.add(velocity);
            velocity.mul(isTouchingWater() ? waterDrag : airDrag);
        }

        // Check if below world
        if (pos.y < mc.world.getBottomY()) return MissHitResult.INSTANCE;

        // Check if chunk is loaded
        int chunkX = ChunkSectionPos.getSectionCoord(pos.x);
        int chunkZ = ChunkSectionPos.getSectionCoord(pos.z);
        if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return MissHitResult.INSTANCE;

        // Check for collision
        ((IVec3d) pos3d).meteor$set(pos);
        if (pos3d.equals(prevPos3d)) return MissHitResult.INSTANCE;

        HitResult hitResult = getCollision();
        return hitResult.getType() == HitResult.Type.MISS ? null : hitResult;
    }

    /**
     * {@link Entity#updateMovementInFluid(TagKey, double)}
     */
    public boolean isTouchingWater() {
        Box box = dimensions.getBoxAt(pos.x, pos.y, pos.z).contract(0.001);
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = mc.world.getFluidState(blockPos);
                    if (fluidState.isIn(FluidTags.WATER)) {
                        double fluidY = y + fluidState.getHeight(mc.world, blockPos);
                        if (fluidY >= box.minY) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * {@link ProjectileUtil#getCollision(Vec3d, Entity, java.util.function.Predicate, Vec3d, net.minecraft.world.World, float, RaycastContext.ShapeType)}
     *
     * Vanilla checks from the current to the next position. We check from the previous to the current positions - it
     * solves the issue of the collision check from the starting position not working properly
     */
    private HitResult getCollision() {
        HitResult hitResult = mc.world.raycast(new RaycastContext(
            prevPos3d,
            pos3d,
            RaycastContext.ShapeType.COLLIDER,
            waterDrag == 0 ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
            simulatingEntity
        ));
        if (hitResult.getType() != HitResult.Type.MISS) {
            ((IVec3d) pos3d).meteor$set(hitResult.getPos());
        }

        HitResult hitResult2 = ProjectileUtil.getEntityCollision(
            mc.world,
            simulatingEntity,
            prevPos3d,
            pos3d,
            dimensions.getBoxAt(pos3d).stretch(velocity.x, velocity.y, velocity.z).expand(1.0D),
            entity -> !entity.isSpectator() && entity.isAlive() && entity.canHit(),
            ProjectileUtil.getToleranceMargin(simulatingEntity)
        );
        if (hitResult2 != null) {
            hitResult = hitResult2;
        }

        return hitResult;
    }
}
