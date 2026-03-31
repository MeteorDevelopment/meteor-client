/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Camera;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;

public class ClickTP extends Module {

    public ClickTP() {
        super(Categories.Movement, "click-tp", "Teleports you to the block you click on.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.getInventory().getSelectedStack().getUseAction() != ItemUseAnimation.NONE) return;
        if (!mc.options.useKey.isPressed()) return;

        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == BlockHitResult.Type.ENTITY && mc.player.interact(((EntityHitResult) mc.crosshairTarget).getEntity(), InteractionHand.MAIN_HAND) != InteractionResult.PASS)
                return;
            if (mc.crosshairTarget.getType() == BlockHitResult.Type.BLOCK && mc.player.getMainHandStack().getItem() instanceof BlockItem)
                return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3 cameraPos = camera.getCameraPos();

        // Calculate the direction the camera is looking based on its pitch and yaw, and extend this direction 210 units away from the camera position
        // 210 is used here as the maximum distance for this exploit is 200 blocks
        // This is done to be able to click tp while in freecam
        Vec3 direction = Vec3.fromPolar(camera.getPitch(), camera.getYaw()).multiply(210);
        Vec3 targetPos = cameraPos.add(direction);

        ClipContext context = new RaycastContext(
            cameraPos,   // start position of the ray
            targetPos,   // end position of the ray
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            mc.player
        );

        BlockHitResult hitResult = mc.world.raycast(context);

        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            Direction side = hitResult.getSide();

            if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, hitResult) != InteractionResult.PASS) return;

            BlockState state = mc.world.getBlockState(pos);

            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

            double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

            Vec3 newPos = new Vec3d(pos.getX() + 0.5 + side.getOffsetX(), pos.getY() + height, pos.getZ() + 0.5 + side.getOffsetZ());
            int packetsRequired = (int) Math.ceil(mc.player.getEntityPos().distanceTo(newPos) / 10) - 1; // subtract 1 to account for the final packet with movement
            if (packetsRequired > 19) packetsRequired = 0;

            for (int packetNumber = 0; packetNumber < (packetsRequired); packetNumber++) {
                mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.OnGroundOnly(true, true));
            }

            mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, true, true));
            mc.player.setPosition(newPos);
        }
    }
}
