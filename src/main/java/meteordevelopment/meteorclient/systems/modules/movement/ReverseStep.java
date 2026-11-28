/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;

public class ReverseStep extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast to fall in blocks per second.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("The maximum fall distance this setting will activate at.")
        .defaultValue(3)
        .min(0)
        .build()
    );

    private final Setting<Boolean> vehicles = sgGeneral.add(new BoolSetting.Builder()
        .name("vehicles")
        .description("Whether or not reverse step should affect vehicles.")
        .defaultValue(false)
        .build()
    );

    public ReverseStep() {
        super(Categories.Movement, "reverse-step", "Allows you to fall down blocks at a greater speed.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Entity vehicle = mc.player.getVehicle();
        if (vehicle != null && vehicles.get()) {
            if (canSnap(vehicle)) {
                ((IVec3) vehicle.getDeltaMovement()).meteor$setY(-fallSpeed.get());
            }
        } else {
            if (mc.player.isSuppressingSlidingDownLadder() || mc.player.zza == 0 && mc.player.xxa == 0) return;
            if (!isOnBed() && canSnap(mc.player)) {
                ((IVec3) mc.player.getDeltaMovement()).meteor$setY(-fallSpeed.get());
            }
        }
    }

    private boolean canSnap(Entity entity) {
        if (!entity.onGround() || entity.isUnderWater() || entity.isInLava() || mc.options.keyJump.isDown() || entity.noPhysics)
            return false;
        return !mc.level.noCollision(entity.getBoundingBox().move(0.0, (float) -(fallDistance.get() + 0.01), 0.0));
    }

    private boolean isOnBed() {
        BlockPos.MutableBlockPos blockPos = mc.player.blockPosition().mutable();

        if (check(blockPos, 0, 0)) return true;

        double xa = mc.player.getX() - blockPos.getX();
        double za = mc.player.getZ() - blockPos.getZ();

        if (xa >= 0 && xa <= 0.3 && check(blockPos, -1, 0)) return true;
        if (xa >= 0.7 && check(blockPos, 1, 0)) return true;
        if (za >= 0 && za <= 0.3 && check(blockPos, 0, -1)) return true;
        if (za >= 0.7 && check(blockPos, 0, 1)) return true;

        if (xa >= 0 && xa <= 0.3 && za >= 0 && za <= 0.3 && check(blockPos, -1, -1)) return true;
        if (xa >= 0 && xa <= 0.3 && za >= 0.7 && check(blockPos, -1, 1)) return true;
        if (xa >= 0.7 && za >= 0 && za <= 0.3 && check(blockPos, 1, -1)) return true;
        return xa >= 0.7 && za >= 0.7 && check(blockPos, 1, 1);
    }

    private boolean check(BlockPos.MutableBlockPos blockPos, int x, int z) {
        blockPos.move(x, 0, z);
        boolean is = mc.level.getBlockState(blockPos).getBlock() instanceof BedBlock;
        blockPos.move(-x, 0, -z);

        return is;
    }
}
