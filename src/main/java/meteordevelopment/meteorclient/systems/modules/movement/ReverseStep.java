/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
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

    public ReverseStep() {
        super(Categories.Movement, "reverse-step", "Allows you to fall down blocks at a greater speed.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!mc.player.isOnGround() || mc.player.isHoldingOntoLadder() || mc.player.isSubmergedInWater() || mc.player.isInLava() ||mc.options.jumpKey.isPressed() || mc.player.noClip || mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) return;

        if (!isOnBed() && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, (float) -(fallDistance.get() + 0.01), 0.0))) ((IVec3d) mc.player.getVelocity()).setY(-fallSpeed.get());
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
