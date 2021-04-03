/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
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
        if (!mc.options.keyUse.isPressed() || mc.player.isSneaking()) return;

        for (BlockEntity b : mc.world.blockEntities) {
            if (new BlockPos(mc.player.raycast(mc.interactionManager.getReachDistance(), mc.getTickDelta(), false).getPos()).equals(b.getPos())) return;
        }

        Vec3d nextPos = new Vec3d(0, 0, 0.1)
                .rotateX(-(float) Math.toRadians(mc.player.pitch))
                .rotateY(-(float) Math.toRadians(mc.player.yaw));

        for (int i = 1; i < mc.interactionManager.getReachDistance()*10; i++) {
            BlockPos curPos = new BlockPos(mc.player.getCameraPosVec(mc.getTickDelta()).add(nextPos.multiply(i)));
            if (posList.contains(curPos)) continue;
            posList.add(curPos);

            for (BlockEntity b : mc.world.blockEntities) {
                if (b.getPos().equals(curPos)) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND,
                            new BlockHitResult(mc.player.getPos(), Direction.UP, curPos, true));
                    return;
                }
            }
        }

        posList.clear();
    }
}
