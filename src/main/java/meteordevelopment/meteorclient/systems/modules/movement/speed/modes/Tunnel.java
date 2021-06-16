/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Tunnel extends SpeedMode {

    public Tunnel() {
        super(SpeedModes.Tunnel);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        Block blockAbove = mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() + 2, mc.player.getZ())).getBlock();
        Block blockBelow = mc.world.getBlockState(new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())).getBlock();

        if (Speed.isAboveValid(blockAbove) && Speed.isBelowValid(blockBelow) && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
            float yaw = (float)Math.toRadians(mc.player.getYaw());
            if (mc.options.keyForward.isPressed() && !mc.player.isSneaking() && mc.player.isOnGround()) {
                ((IVec3d) mc.player.getVelocity()).setXZ(mc.player.getVelocity().x - ((double)MathHelper.sin(yaw) * settings.tunnelSpeed.get()), mc.player.getVelocity().z + ((double)MathHelper.cos(yaw) * settings.tunnelSpeed.get()));
            }
        }
    }
}
