/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClickTP extends Module {

    public ClickTP() {
        super(Categories.Movement, "click-tp", "Teleports you to the block you click on.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.getInventory().getSelectedItem().getUseAnimation() != ItemUseAnimation.NONE) return;
        if (!mc.options.keyUse.isDown()) return;

        if (mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.ENTITY && mc.player.interactOn(((EntityHitResult) mc.hitResult).getEntity(), InteractionHand.MAIN_HAND, ((EntityHitResult) mc.hitResult).getLocation()) != InteractionResult.PASS) return;
            if (mc.hitResult.getType() == HitResult.Type.BLOCK && mc.player.getMainHandItem().getItem() instanceof BlockItem) return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        // Calculate the direction the camera is looking based on its pitch and yaw, and extend this direction 210 units away from the camera position
        // 210 is used here as the maximum distance for this exploit is 200 blocks
        // This is done to be able to click tp while in freecam
        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(210);
        Vec3 targetPos = cameraPos.add(direction);

        ClipContext context = new ClipContext(
            cameraPos,   // start position of the ray
            targetPos,   // end position of the ray
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            mc.player
        );

        BlockHitResult hitResult = mc.level.clip(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            Direction side = hitResult.getDirection();

            if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, hitResult) != InteractionResult.PASS) return;

            BlockState state = mc.level.getBlockState(pos);

            VoxelShape shape = state.getCollisionShape(mc.level, pos);
            if (shape.isEmpty()) shape = state.getShape(mc.level, pos);

            double height = shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y);

            Vec3 newPos = new Vec3(pos.getX() + 0.5 + side.getStepX(), pos.getY() + height, pos.getZ() + 0.5 + side.getStepZ());
            int packetsRequired = (int) Math.ceil(mc.player.position().distanceTo(newPos) / 10) - 1; // subtract 1 to account for the final packet with movement
            if (packetsRequired > 19) packetsRequired = 0;

            for (int packetNumber = 0; packetNumber < (packetsRequired); packetNumber++) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
            }

            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, true, true));
            mc.player.setPos(newPos);
        }
    }
}
