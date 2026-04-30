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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class GhostHand extends Module {
    private final Set<BlockPos> posList = new ObjectOpenHashSet<>();

    public GhostHand() {
        super(Categories.Player, "ghost-hand", "Opens containers through walls.");
    }

    @EventHandler
    private void onTick(DoItemUseEvent event) {
        if (!mc.options.keyUse.isDown() || mc.player.isShiftKeyDown()) return;

        if (mc.level.getBlockState(BlockPos.containing(mc.player.pick(mc.player.blockInteractionRange(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false).getLocation())).hasBlockEntity())
            return;

        Vec3 direction = new Vec3(0, 0, 0.1)
            .xRot(-(float) Math.toRadians(mc.player.getXRot()))
            .yRot(-(float) Math.toRadians(mc.player.getYRot()));

        posList.clear();

        for (int i = 1; i < mc.player.blockInteractionRange() * 10; i++) {
            BlockPos pos = BlockPos.containing(mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)).add(direction.scale(i)));

            if (posList.contains(pos)) continue;
            posList.add(pos);

            if (mc.level.getBlockState(pos).hasBlockEntity()) {
                for (InteractionHand hand : InteractionHand.values()) {
                    InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
                    if (result instanceof InteractionResult.Success || result instanceof InteractionResult.Fail) {
                        mc.player.swing(hand);
                        event.cancel();
                        return;
                    }
                }
            }
        }
    }
}
