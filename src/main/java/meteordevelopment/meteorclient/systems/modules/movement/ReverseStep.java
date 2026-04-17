/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

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
                ((IVec3d) vehicle.getVelocity()).meteor$setY(-fallSpeed.get());
            }
        } else {
            if (mc.player.isHoldingOntoLadder() || mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) return;
            if (!isOnBed() && canSnap(mc.player)) {
                ((IVec3d)mc.player.getVelocity()).meteor$setY(-fallSpeed.get());
            }
        }
    }

    private boolean canSnap(Entity entity) {
        if (!entity.isOnGround() || entity.isSubmergedInWater() || entity.isInLava() || mc.options.jumpKey.isPressed() || entity.noClip)
            return false;
        return !mc.world.isSpaceEmpty(entity.getBoundingBox().offset(0.0, (float) -(fallDistance.get() + 0.01), 0.0));
    }

    private boolean isOnBed() {
        BlockPos.Mutable blockPos = mc.player.getBlockPos().mutableCopy();

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

    private boolean check(BlockPos.Mutable blockPos, int x, int z) {
        blockPos.move(x, 0, z);
        boolean is = mc.world.getBlockState(blockPos).getBlock() instanceof BedBlock;
        blockPos.move(-x, 0, -z);

        return is;
    }
}
