/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class BowAimbot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to aim at it.")
        .defaultValue(20)
        .range(0, 100)
        .sliderMax(100)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("What type of entities to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
        .name("babies")
        .description("Whether or not to attack baby variants of the entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
        .name("nametagged")
        .description("Whether or not to attack mobs with a name tag.")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-combat")
        .description("Freezes Baritone temporarily until you released the bow.")
        .defaultValue(false)
        .build()
    );

    private boolean wasPathing;
    private Entity target;

    public BowAimbot() {
        super(Categories.Combat, "bow-aimbot", "Automatically aims your bow for you.");
    }

    @Override
    public void onDeactivate() {
        target = null;
        wasPathing = false;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!PlayerUtils.isAlive() || !itemInHand()) return;
        if (!mc.player.getAbilities().creativeMode && !InvUtils.find(itemStack -> itemStack.getItem() instanceof ArrowItem).found()) return;

        target = TargetUtils.get(entity -> {
            if (entity == mc.player || entity == mc.cameraEntity) return false;
            if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
            if (!PlayerUtils.isWithin(entity, range.get())) return false;
            if (!entities.get().contains(entity.getType())) return false;
            if (!nametagged.get() && entity.hasCustomName()) return false;
            if (!PlayerUtils.canSeeEntity(entity)) return false;
            if (entity instanceof PlayerEntity) {
                if (((PlayerEntity) entity).isCreative()) return false;
                if (!Friends.get().shouldAttack((PlayerEntity) entity)) return false;
            }
            return !(entity instanceof AnimalEntity) || babies.get() || !((AnimalEntity) entity).isBaby();
        }, priority.get());

        if (target == null) {
            if (wasPathing) {
                PathManagers.get().resume();
                wasPathing = false;
            }
            return;
        }

        if (mc.options.useKey.isPressed() && itemInHand()) {
            if (pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
                PathManagers.get().pause();
                wasPathing = true;
            }
            aim(event.tickDelta);
        }
    }

    private boolean itemInHand() {
        return InvUtils.testInMainHand(Items.BOW, Items.CROSSBOW);
    }

    private void aim(float tickDelta) {
        // Velocity based on bow charge.
        float velocity = BowItem.getPullProgress(mc.player.getItemUseTime());

        // Positions
        Vec3d pos = target.getLerpedPos(tickDelta);

        double relativeX = pos.x - mc.player.getX();
        double relativeY = pos.y + (target.getHeight() / 2) - mc.player.getEyeY();
        double relativeZ = pos.z - mc.player.getZ();

        // Calculate the pitch
        double hDistance = Math.sqrt(relativeX * relativeX + relativeZ * relativeZ);
        double hDistanceSq = hDistance * hDistance;
        float g = 0.006f;
        float velocitySq = velocity * velocity;
        float pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(velocitySq * velocitySq - g * (g * hDistanceSq + 2 * relativeY * velocitySq))) / (g * hDistance)));

        // Set player rotation
        if (Float.isNaN(pitch)) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target));
        } else {
            Rotations.rotate(Rotations.getYaw(new Vec3d(pos.x, pos.y, pos.z)), pitch);
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
