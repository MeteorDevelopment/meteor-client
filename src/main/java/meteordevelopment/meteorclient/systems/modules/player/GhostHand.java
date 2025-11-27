/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.entity.player.DoItemUseEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class GhostHand extends Module {
    private final Set<BlockPos> posList = new ObjectOpenHashSet<>();

    public GhostHand() {
        super(Categories.Player, "ghost-hand", "Opens containers through walls.");
    }

    @EventHandler
    private void onTick(DoItemUseEvent event) {
        if (!mc.options.useKey.isPressed() || mc.player.isSneaking()) return;

        if (mc.world.getBlockState(BlockPos.ofFloored(mc.player.raycast(mc.player.getBlockInteractionRange(), mc.getRenderTickCounter().getTickProgress(true), false).getPos())).hasBlockEntity()) return;

        Vec3d direction = new Vec3d(0, 0, 0.1)
                .rotateX(-(float) Math.toRadians(mc.player.getPitch()))
                .rotateY(-(float) Math.toRadians(mc.player.getYaw()));

        posList.clear();

        for (int i = 1; i < mc.player.getBlockInteractionRange() * 10; i++) {
            BlockPos pos = BlockPos.ofFloored(mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickProgress(true)).add(direction.multiply(i)));

            if (posList.contains(pos)) continue;
            posList.add(pos);

            if (mc.world.getBlockState(pos).hasBlockEntity()) {
                for (Hand hand : Hand.values()) {
                    ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
                    if (result instanceof ActionResult.Success || result instanceof ActionResult.Fail) {
                        mc.player.swingHand(hand);
                        event.cancel();
                        return;
                    }
                }
            }
        }
    }
}
