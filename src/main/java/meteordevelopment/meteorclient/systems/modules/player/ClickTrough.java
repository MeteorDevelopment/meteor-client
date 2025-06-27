/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * @author NDev007
 * @since 27.06.2025
 */
public class ClickTrough extends Module {
    private final Set<BlockPos> checkedPositions = new HashSet<>();

    public ClickTrough() {
        super(Categories.Player, "click-trough", "Allows you to click through blocks to entities (such as chests).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (!mc.options.useKey.isPressed() || mc.player.isSneaking()) return;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        checkedPositions.clear();

        for (int i = 1; i < 50; i++) {
            Vec3d pos = eyePos.add(lookVec.multiply(i * 0.5));
            BlockPos blockPos = BlockPos.ofFloored(pos);
            if (!checkedPositions.add(blockPos)) continue;
            BlockEntity be = mc.world.getBlockEntity(blockPos);
            if (be != null) {
                Vec3d hit = Vec3d.ofCenter(blockPos).add(0, 0.5, 0);
                BlockHitResult bhr = new BlockHitResult(hit, Direction.UP, blockPos, true);
                BlockUtils.interact(bhr, Hand.MAIN_HAND, true);
                break;
            }
        }
    }
}
