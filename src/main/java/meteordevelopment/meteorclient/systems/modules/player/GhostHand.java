/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class GhostHand extends Module {
    private final List<BlockPos> posList = new ArrayList<>();

    public GhostHand() {
        super(Categories.Player, "ghost-hand", "Opens containers through walls.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.options.useKey.isPressed() || mc.player.isSneaking()) return;

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (new BlockPos(mc.player.raycast(mc.interactionManager.getReachDistance(), mc.getTickDelta(), false).getPos()).equals(blockEntity.getPos())) return;
        }

        Vec3d nextPos = new Vec3d(0, 0, 0.1)
                .rotateX(-(float) Math.toRadians(mc.player.getPitch()))
                .rotateY(-(float) Math.toRadians(mc.player.getYaw()));

        for (int i = 1; i < mc.interactionManager.getReachDistance() * 10; i++) {
            BlockPos curPos = new BlockPos(mc.player.getCameraPosVec(mc.getTickDelta()).add(nextPos.multiply(i)));

            if (posList.contains(curPos)) continue;
            posList.add(curPos);

            for (BlockEntity blockEntity : Utils.blockEntities()) {
                if (blockEntity.getPos().equals(curPos)) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, curPos, true));
                    return;
                }
            }
        }

        posList.clear();
    }
}
