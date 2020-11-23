/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import com.google.common.collect.Streams;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class AimAssist extends ToggleModule {
    public enum Priority {
        LowestDistance,
        HighestDistance,
        LowestHealth,
        HighestHealth
    }

    public enum Target {
        Eyes,
        Body,
        Feet
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");

    // General
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Aim range.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to aim at.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Aim at friends, useful only if attack players is on.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Aim through walls.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
            .name("priority")
            .description("What entities to target.")
            .defaultValue(Priority.LowestHealth)
            .build()
    );

    private final Setting<Target> target = sgGeneral.add(new EnumSetting.Builder<Target>()
            .name("target")
            .description("Where to aim.")
            .defaultValue(Target.Body)
            .build()
    );

    // Speed
    private final Setting<Boolean> speedInstant = sgSpeed.add(new BoolSetting.Builder()
            .name("speed-instant")
            .description("Instantly looks at the entity.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How fast to aim at the entity.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Vec3d vec3d1 = new Vec3d(0, 0, 0);
    private final Vec3d vec3d2 = new Vec3d(0, 0, 0);

    private Entity entity;

    public AimAssist() {
        super(Category.Combat, "aim-assist", "Automatically aims at entities.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        entity = null;

        Streams.stream(mc.world.getEntities())
                .filter(entity -> mc.player.distanceTo(entity) <= range.get())
                .filter(this::canAttackEntity)
                .filter(this::canSeeEntity)
                .filter(Entity::isAlive)
                .min(this::sort)
                .ifPresent(entity -> this.entity = entity);
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (entity == null) return;

        if (speedInstant.get()) aimInstantly();
        else aim(event.tickDelta);
    });

    private void aimInstantly() {
        setVec3dToTargetPoint(vec3d1, entity);

        double deltaX = vec3d1.x - mc.player.getX();
        double deltaZ = vec3d1.z - mc.player.getZ();
        double deltaY = vec3d1.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        // Yaw
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        mc.player.yaw = (float) angle;

        // Pitch
        double idk = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, idk));
        mc.player.pitch = (float) angle;
    }

    private void aim(double delta) {
        setVec3dToTargetPoint(vec3d1, entity);

        double deltaX = vec3d1.x - mc.player.getX();
        double deltaZ = vec3d1.z - mc.player.getZ();
        double deltaY = vec3d1.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        // Yaw
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double deltaAngle = MathHelper.wrapDegrees(angle - mc.player.yaw);
        double toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
        if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
        mc.player.yaw += toRotate;

        // Pitch
        double idk = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, idk));
        deltaAngle = MathHelper.wrapDegrees(angle - mc.player.pitch);
        toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
        if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
        mc.player.pitch += toRotate;
    }

    private void setVec3dToTargetPoint(Vec3d vec3d, Entity entity) {
        switch (target.get()) {
            case Eyes: ((IVec3d) vec3d).set(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ()); break;
            case Body: ((IVec3d) vec3d).set(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()) / 2, entity.getZ()); break;
            case Feet: ((IVec3d) vec3d).set(entity.getX(), entity.getY(), entity.getZ()); break;
        }
    }

    private boolean canAttackEntity(Entity entity) {
        if (entity == mc.player || !entities.get().contains(entity.getType())) return false;

        if (entity instanceof PlayerEntity) {
            if (friends.get()) return true;
            return FriendManager.INSTANCE.attack((PlayerEntity) entity);
        }

        return true;
    }

    private boolean canSeeEntity(Entity entity) {
        if (ignoreWalls.get()) return true;

        ((IVec3d) vec3d1).set(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
        ((IVec3d) vec3d2).set(entity.getX(), entity.getY(), entity.getZ());
        boolean canSeeFeet =  mc.world.raycast(new RaycastContext(vec3d1, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        ((IVec3d) vec3d2).set(entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ());
        boolean canSeeEyes =  mc.world.raycast(new RaycastContext(vec3d1, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;

        return canSeeFeet || canSeeEyes;
    }

    private int sort(Entity e1, Entity e2) {
        switch (priority.get()) {
            case LowestDistance:  return Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player));
            case HighestDistance: return invertSort(Double.compare(e1.distanceTo(mc.player), e2.distanceTo(mc.player)));
            case LowestHealth: {
                float a = e1 instanceof LivingEntity ? ((LivingEntity) e1).getHealth() : 0;
                float b = e2 instanceof LivingEntity ? ((LivingEntity) e2).getHealth() : 0;
                return Float.compare(a, b);
            }
            case HighestHealth: {
                float a = e1 instanceof LivingEntity ? ((LivingEntity) e1).getHealth() : 0;
                float b = e2 instanceof LivingEntity ? ((LivingEntity) e2).getHealth() : 0;
                return invertSort(Float.compare(a, b));
            }
            default: return 0;
        }
    }

    private int invertSort(int sort) {
        if (sort == 0) return 0;
        return sort > 0 ? -1 : 1;
    }
}
