/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.item.BlockItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

public class ClickTP extends Module {

    public ClickTP() {
        super(Categories.Movement, "click-tp", "Teleports you to the block you click on.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.getInventory().getSelectedStack().getUseAction() != UseAction.NONE) return;
        if (!mc.options.useKey.isPressed()) return;

        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.player.interact(((EntityHitResult) mc.crosshairTarget).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) return;
            if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK && mc.player.getMainHandStack().getItem() instanceof BlockItem) return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        // Calculate the direction the camera is looking based on its pitch and yaw, and extend this direction 210 units away from the camera position
        // 210 is used here as the maximum distance for this exploit is 200 blocks
        // This is done to be able to click tp while in freecam
        Vec3d direction = Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(210);
        Vec3d targetPos = cameraPos.add(direction);

        RaycastContext context = new RaycastContext(
            cameraPos,   // start position of the ray
            targetPos,   // end position of the ray
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );

        BlockHitResult hitResult = mc.world.raycast(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            Direction side = hitResult.getSide();

            if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, hitResult) != ActionResult.PASS) return;

            BlockState state = mc.world.getBlockState(pos);

            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

            double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

            Vec3d newPos = new Vec3d(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + height, pos.getZ() + 0.5 + side.getOffsetZ());
            int packetsRequired = (int) Math.ceil(mc.player.getPos().distanceTo(newPos) / 10) - 1; // subtract 1 to account for the final packet with movement
            if (packetsRequired > 19) packetsRequired = 0;

            for (int packetNumber = 0; packetNumber < (packetsRequired); packetNumber++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, true));
            }

            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, true, true));
            mc.player.setPosition(newPos);
        }
    }
}
