/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.Target;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AimAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Aim Speed");

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range at which an entity can be targeted.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to aim at.")
            .defaultValue(Utils.asObject2BooleanOpenHashMap(EntityType.PLAYER))
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Whether or not to aim at friends.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Whether or not to ignore aiming through walls.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("How to select target from entities in range.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private final Setting<Target> bodyTarget = sgGeneral.add(new EnumSetting.Builder<Target>()
            .name("target")
            .description("Which part of the entities body to aim at.")
            .defaultValue(Target.Body)
            .build()
    );

    // Aim Speed

    private final Setting<Boolean> instant = sgSpeed.add(new BoolSetting.Builder()
            .name("instant-look")
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
    private Entity target;

    public AimAssist() {
        super(Category.Combat, "aim-assist", "Automatically aims at entities.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        target = EntityUtils.get(entity -> {
            if (!entity.isAlive()) return false;
            if (mc.player.distanceTo(entity) >= range.get()) return false;
            if (!canSeeEntity(entity)) return false;
            if (entity == mc.player || !entities.get().getBoolean(entity.getType())) return false;

            if (entity instanceof PlayerEntity && !friends.get()) {
                return Friends.get().attack((PlayerEntity) entity);
            }

            return true;
            }, priority.get());
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (target != null) aim(target, event.tickDelta, instant.get());
    }

    private void aim(Entity target, double delta, boolean instant) {
        setVec3dToTargetPoint(vec3d1, target);

        double deltaX = vec3d1.x - mc.player.getX();
        double deltaZ = vec3d1.z - mc.player.getZ();
        double deltaY = vec3d1.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        // Yaw
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double deltaAngle;
        double toRotate;

        if (instant) {
            mc.player.yaw = (float) angle;
        } else {
            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.yaw);
            toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
            mc.player.yaw += toRotate;
        }

        // Pitch
        double idk = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, idk));

        if (instant) {
            mc.player.pitch = (float) angle;
        } else {
            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.pitch);
            toRotate = speed.get() * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
            mc.player.pitch += toRotate;
        }
    }

    private void setVec3dToTargetPoint(Vec3d vec3d, Entity entity) {
        switch (bodyTarget.get()) {
            case Head: ((IVec3d) vec3d).set(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ()); break;
            case Body: ((IVec3d) vec3d).set(entity.getX(), entity.getY() + entity.getEyeHeight(entity.getPose()) / 2, entity.getZ()); break;
            case Feet: ((IVec3d) vec3d).set(entity.getX(), entity.getY(), entity.getZ()); break;
        }
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

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
