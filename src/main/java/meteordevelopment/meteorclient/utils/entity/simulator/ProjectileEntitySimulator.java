/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity.simulator;

import meteordevelopment.meteorclient.mixin.CrossbowItemAccessor;
import meteordevelopment.meteorclient.mixin.ProjectileInGroundAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collection;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ProjectileEntitySimulator {
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    private final Vec3 pos3d = new Vec3(0, 0, 0);
    private final Vec3 prevPos3d = new Vec3(0, 0, 0);

    public final Vector3d pos = new Vector3d();
    private final Vector3d velocity = new Vector3d();

    private Projectile simulatingEntity;
    private EntityDimensions dimensions;
    private int tickCount, pierceLevel;
    private double gravity;
    private float airDrag, waterDrag;
    private boolean isInWater;

    public record MotionData(
        float power,
        float roll,
        double gravity,
        float airDrag,
        float waterDrag,
        EntityType<?> entity
    ) {
        public MotionData withPower(float power) {
            return new MotionData(power, this.roll(), this.gravity(), this.airDrag(), this.waterDrag(), this.entity());
        }
    }

    // https://minecraft.wiki/w/Projectile
    // https://minecraft.wiki/w/Entity#Motion

    // ThrowableProjectile
    private static final MotionData EGG = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.EGG);
    private static final MotionData ENDER_PEARL = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.ENDER_PEARL);
    private static final MotionData SNOWBALL = new MotionData(1.5f, 0, 0.03, 0.99f, 0.8f, EntityType.SNOWBALL);
    private static final MotionData EXPERIENCE_BOTTLE = new MotionData(0.7f, -20, 0.07, 0.99f, 0.8f, EntityType.EXPERIENCE_BOTTLE);
    private static final MotionData LINGERING_POTION = new MotionData(0.5f, -20, 0.05, 0.99f, 0.8f, EntityType.LINGERING_POTION);
    private static final MotionData SPLASH_POTION = new MotionData(0.5f, -20, 0.05, 0.99f, 0.8f, EntityType.SPLASH_POTION);

    // AbstractHurtingProjectile
    private static final MotionData EXPLOSIVE = new MotionData(0, 0, 0, 1, 1, null); // fireball, wither skull, etc.
    private static final MotionData WIND_CHARGE = new MotionData(1.5f, 0, 0, 1, 1, EntityType.WIND_CHARGE);

    // AbstractArrow
    private static final MotionData ARROW = new MotionData(0, 0, 0.05, 0.99f, 0.6f, EntityType.ARROW);
    private static final MotionData TRIDENT = new MotionData(2.5f, 0, 0.05, 0.99f, 0.99f, EntityType.TRIDENT);

    // Other
    private static final MotionData FIREWORK_ROCKET = new MotionData(0, 0, 0, 1, 1, EntityType.FIREWORK_ROCKET);
    private static final MotionData FISHING_BOBBER = new MotionData(0, 0, 0.03, 0.92f, 0, EntityType.FISHING_BOBBER);
    private static final MotionData LLAMA_SPIT = new MotionData(1.5f, 0, 0.06, 0.99f, 0, EntityType.LLAMA_SPIT);


    // held items

    public boolean set(Entity user, ItemStack itemStack, double angleOffset, boolean accurate, float tickDelta) {
        Item item = itemStack.getItem();

        switch (item) {
            case BowItem _ -> {
                if (!(user instanceof LivingEntity livingEntity)) return false;
                float charge = BowItem.getPowerForTime(livingEntity.getTicksUsingItem());

                if (charge <= 0.1) {
                    if (user == mc.player) charge = 1;
                    else return false;
                }

                set(user, angleOffset, accurate, tickDelta, ARROW.withPower(charge * 3));
            }
            case CrossbowItem _ -> {
                ChargedProjectiles projectilesComponent = itemStack.get(DataComponents.CHARGED_PROJECTILES);
                if (projectilesComponent == null) return false;

                float speed = CrossbowItemAccessor.meteor$getSpeed(projectilesComponent);
                if (projectilesComponent.contains(Items.FIREWORK_ROCKET)) {
                    set(user, angleOffset, accurate, tickDelta, FIREWORK_ROCKET.withPower(speed));
                } else set(user, angleOffset, accurate, tickDelta, ARROW.withPower(speed));

                this.pierceLevel = projectilesComponent.contains(Items.FIREWORK_ROCKET) ? 0 : Utils.getEnchantmentLevel(itemStack, Enchantments.PIERCING);
            }
            case WindChargeItem _ -> set(user, angleOffset, accurate, tickDelta, WIND_CHARGE);
            case TridentItem _ -> set(user, angleOffset, accurate, tickDelta, TRIDENT);
            case SnowballItem _ -> set(user, angleOffset, accurate, tickDelta, SNOWBALL);
            case EggItem _ -> set(user, angleOffset, accurate, tickDelta, EGG);
            case EnderpearlItem _ -> set(user, angleOffset, accurate, tickDelta, ENDER_PEARL);
            case ExperienceBottleItem _ -> set(user, angleOffset, accurate, tickDelta, EXPERIENCE_BOTTLE);
            case SplashPotionItem _ -> set(user, angleOffset, accurate, tickDelta, SPLASH_POTION);
            case LingeringPotionItem _ -> set(user, angleOffset, accurate, tickDelta, LINGERING_POTION);
            case FishingRodItem _ -> setFishingBobber(user, tickDelta, FISHING_BOBBER);
            default -> {
                return false;
            }
        }

        return true;
    }

    public void set(Entity user, double angleOffset, boolean accurate, float tickDelta, MotionData data) {
        // I lost my mind for an hour trying to figure out why arrows and tridents were spawning lower than expected,
        // and it was because no slow air strict was silently causing the player to crouch AAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        Pose pose = user.getPose();
        if (user == mc.player && (Modules.get().get(NoSlow.class).airStrict() || Modules.get().get(Sneak.class).doPacket()))
            pose = Pose.CROUCHING;
        Utils.set(pos, user, tickDelta).add(0, user.getEyeHeight(pose) - 0.1f, 0);

        double yaw;
        double pitch;

        if (user == mc.player && Rotations.rotating) {
            yaw = Rotations.serverYaw;
            pitch = Rotations.serverPitch;
        } else {
            yaw = user.getYRot(tickDelta);
            pitch = user.getXRot(tickDelta);
        }

        double x, y, z;

        if (angleOffset == 0) {
            x = -Math.sin(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
            y = -Math.sin((pitch + data.roll()) * 0.017453292);
            z = Math.cos(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
        } else {
            Vec3 oppositeRotationVec = user.getUpVector(1.0F);
            Quaterniond quaternion = new Quaterniond().setAngleAxis(angleOffset, oppositeRotationVec.x, oppositeRotationVec.y, oppositeRotationVec.z);
            Vec3 rotationVec = user.getViewVector(1.0F);
            Vector3d vector3d = new Vector3d(rotationVec.x, rotationVec.y, rotationVec.z);
            vector3d.rotate(quaternion);

            x = vector3d.x;
            y = vector3d.y;
            z = vector3d.z;
        }

        velocity.set(x, y, z).normalize().mul(data.power());

        if (accurate) {
            Vec3 vel = user.getKnownMovement();
            velocity.add(vel.x, user.onGround() ? 0.0D : vel.y, vel.z);
        }

        setSimulationData((Projectile) data.entity().create(mc.level, null), data);
    }

    public void setFishingBobber(Entity user, float tickDelta, MotionData data) {
        double yaw;
        double pitch;

        if (user == mc.player && Rotations.rotating) {
            yaw = Rotations.serverYaw;
            pitch = Rotations.serverPitch;
        } else {
            yaw = user.getYRot(tickDelta);
            pitch = user.getXRot(tickDelta);
        }

        double h = Math.cos(-yaw * 0.017453292F - 3.1415927F);
        double i = Math.sin(-yaw * 0.017453292F - 3.1415927F);
        double j = -Math.cos(-pitch * 0.017453292F);
        double k = Math.sin(-pitch * 0.017453292F);

        Pose pose = user.getPose();
        if (user == mc.player && (Modules.get().get(NoSlow.class).airStrict() || Modules.get().get(Sneak.class).doPacket()))
            pose = Pose.CROUCHING;
        Utils.set(pos, user, tickDelta).sub(i * 0.3, 0, h * 0.3).add(0, user.getEyeHeight(pose), 0);

        velocity.set(-i, Mth.clamp(-(k / j), -5, 5), -h);

        double l = velocity.length();
        velocity.mul(0.6 / l + 0.5, 0.6 / l + 0.5, 0.6 / l + 0.5);

        setSimulationData((Projectile) data.entity().create(mc.level, null), data);
    }


    // fired projectiles

    public boolean set(Entity entity) {
        // skip entities in ground
        if (entity instanceof ProjectileInGroundAccessor ppe && ppe.meteor$invokeIsInGround()) return false;

        switch (entity) {
            case Arrow e -> set(e, ARROW);
            case SpectralArrow e -> set(e, ARROW);
            case ThrownTrident e -> set(e, TRIDENT);
            case ThrownEnderpearl e -> set(e, ENDER_PEARL);
            case Snowball e -> set(e, SNOWBALL);
            case ThrownEgg e -> set(e, EGG);
            case ThrownExperienceBottle e -> set(e, EXPERIENCE_BOTTLE);
            case ThrownSplashPotion e -> set(e, SPLASH_POTION);
            case ThrownLingeringPotion e -> set(e, LINGERING_POTION);
            case AbstractWindCharge e -> set(e, WIND_CHARGE);
            case AbstractHurtingProjectile e -> set(e, EXPLOSIVE);
            case LlamaSpit e -> set(e, LLAMA_SPIT);
            default -> {
                return false;
            }
        }

        if (entity.isNoGravity()) {
            this.gravity = 0;
        }

        return true;
    }

    public void set(Projectile entity, MotionData data) {
        pos.set(entity.getX(), entity.getY(), entity.getZ());

        double speed = entity.getDeltaMovement().length();
        velocity.set(entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z).normalize().mul(speed);

        setSimulationData(entity, data);
    }

    private void setSimulationData(Projectile entity, MotionData data) {
        this.gravity = data.gravity();
        this.airDrag = data.airDrag();
        this.waterDrag = data.waterDrag();
        this.simulatingEntity = entity;
        this.dimensions = simulatingEntity.getDimensions(simulatingEntity.getPose());
        this.isInWater = simulatingEntity.isInWater();
        this.tickCount = simulatingEntity.tickCount;
        this.pierceLevel = 0;
    }

    public SimulationStep tick() {
        tickCount++;
        ((IVec3) prevPos3d).meteor$set(pos);

        // gravity -> drag -> position
        if (simulatingEntity instanceof ThrowableProjectile || simulatingEntity instanceof AbstractHurtingProjectile) {
            velocity.sub(0, gravity, 0);
            velocity.mul(isInWater ? waterDrag : airDrag);
            pos.add(velocity);
            tickIsTouchingWater();
        }
        // position -> drag -> gravity
        else if (simulatingEntity instanceof AbstractArrow || simulatingEntity instanceof LlamaSpit) {
            pos.add(velocity);
            velocity.mul(isInWater ? waterDrag : airDrag);
            velocity.sub(0, gravity, 0);
            tickIsTouchingWater();
        }
        // gravity -> position > drag
        // accurate for fishing bobbers and firework rockets, will need to revisit if more projectiles are added
        else if (simulatingEntity instanceof Projectile) {
            tickIsTouchingWater();
            velocity.sub(0, gravity, 0);
            pos.add(velocity);
            velocity.mul(isInWater ? waterDrag : airDrag);
        }

        // Check if below world
        if (pos.y < mc.level.getMinY()) return SimulationStep.MISS;

        // Check if chunk is loaded
        int chunkX = SectionPos.posToSectionCoord(pos.x);
        int chunkZ = SectionPos.posToSectionCoord(pos.z);
        if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) return SimulationStep.MISS;

        // Check for collision
        ((IVec3) pos3d).meteor$set(pos);
        if (pos3d.equals(prevPos3d)) return SimulationStep.MISS;

        return getCollision();
    }

    /**
     * {@link net.minecraft.world.entity.EntityFluidInteraction#update(Entity, boolean)}
     */
    public void tickIsTouchingWater() {
        AABB box = dimensions.makeBoundingBox(pos.x, pos.y, pos.z).deflate(0.001);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPos.set(x, y, z);
                    FluidState fluidState = mc.level.getFluidState(blockPos);
                    if (fluidState.is(FluidTags.WATER)) {
                        double fluidY = y + fluidState.getHeight(mc.level, blockPos);
                        if (fluidY >= box.minY) {
                            isInWater = true;
                            return;
                        }
                    }
                }
            }
        }

        isInWater = false;
    }

    /**
     * {@link net.minecraft.world.entity.projectile.ProjectileUtil#getHitResult(Vec3, Entity, java.util.function.Predicate, Vec3, net.minecraft.world.level.Level, float, ClipContext.Block)}
     * <p>
     * Vanilla checks from the current to the next position, while we check from the previous to the current positions.
     * This solves the issue of the collision check from the starting position not working properly - otherwise, the
     * simulated projectile may move from its start position through a block, only running the collision check afterwards.
     * The vanilla game has other code to deal with this but this is the easiest way for us to fix it.
     */
    private SimulationStep getCollision() {
        HitResult blockCollision = mc.level.clipIncludingBorder(new ClipContext(
            prevPos3d,
            pos3d,
            ClipContext.Block.COLLIDER,
            waterDrag == 0 ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
            simulatingEntity
        ));
        if (blockCollision.getType() != HitResult.Type.MISS) {
            ((IVec3) pos3d).meteor$set(blockCollision.getLocation());
        }

        /// {@link AbstractArrow#stepMoveAndHit(BlockHitResult)}
        if (simulatingEntity instanceof AbstractArrow) {
            Collection<EntityHitResult> entityCollisions = ProjectileUtil.getManyEntityHitResult(
                mc.level,
                simulatingEntity,
                prevPos3d,
                pos3d,
                dimensions.makeBoundingBox(prevPos3d).expandTowards(velocity.x, velocity.y, velocity.z).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
                getToleranceMargin(),
                ClipContext.Block.COLLIDER,
                false
            );

            // prevent simulating projectiles as colliding with ourselves on the first tick of movement
            entityCollisions.removeIf(collision -> tickCount <= 1 && collision.getEntity() == mc.player);
            if (entityCollisions.isEmpty()) return new SimulationStep(hitOrDeflect(blockCollision), blockCollision);

            boolean stop = false;
            ArrayList<EntityHitResult> hits = new ArrayList<>();
            for (EntityHitResult result : entityCollisions) {
                boolean hit = hitOrDeflect(result);
                if (!hit) break;

                hits.add(result);
                if (pierceLevel <= 0) {
                    stop = true;
                    break;
                }

                pierceLevel--;
            }

            return new SimulationStep(stop, hits.toArray(new HitResult[0]));
        } else {
            HitResult entityCollision = ProjectileUtil.getEntityHitResult(
                mc.level,
                simulatingEntity,
                prevPos3d,
                pos3d,
                dimensions.makeBoundingBox(prevPos3d).expandTowards(velocity.x, velocity.y, velocity.z).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
                getToleranceMargin()
            );

            if (entityCollision == null || (tickCount <= 1 && entityCollision instanceof EntityHitResult ehr && ehr.getEntity() == mc.player)) {
                return new SimulationStep(hitOrDeflect(blockCollision), blockCollision);
            }

            if (hitOrDeflect(entityCollision)) return new SimulationStep(true, entityCollision);
            return new SimulationStep(false);
        }
    }

    /**
     * {@link Projectile#hitTargetOrDeflectSelf(HitResult)}
     * {@link Projectile#onHit(HitResult)}
     * {@link ProjectileDeflection}
     */
    private boolean hitOrDeflect(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            Utils.set(pos, entityHitResult.getLocation());

            if ((entity instanceof Breeze && !(simulatingEntity instanceof AbstractWindCharge)) || entity.deflection(simulatingEntity) == ProjectileDeflection.REVERSE) {
                velocity.mul(-0.5);
                return false;
            }

            // if we keep this it makes trajectories look awful when you throw wind charges
//            if (entity.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectileEntity) {
//                Utils.set(velocity, projectileEntity.getRotationVector());
//                return false;
//            }

            // not perfectly accurate but you would otherwise have to trace significant amounts of the damage stack
            if (entity instanceof LivingEntity livingEntity && livingEntity.isBlocking() && simulatingEntity instanceof AbstractArrow) {
                velocity.mul(-0.5).mul(0.2);
                return velocity.lengthSquared() < 1.0E-7;
            }

            return true;
        } else if (hitResult instanceof BlockHitResult bhr) {
            Utils.set(pos, bhr.getLocation());

            if (simulatingEntity.shouldBounceOnWorldBorder() && bhr.isWorldBorderHit()) {
                velocity.mul(-0.5).mul(0.2);
                return false;
            }

            return bhr.getType() != HitResult.Type.MISS;
        }

        return false;
    }

    private float getToleranceMargin() {
        return Math.clamp((tickCount - 2) / 20.0F, 0.0F, 0.3F);
    }
}
