/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.impl;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.interaction.api.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DefaultInteractionManager implements InteractionManager {
    private final Logger log = LoggerFactory.getLogger(DefaultInteractionManager.class);
    private final List<DefaultAction> actions = new ArrayList<>();

    @Override
    public BlockAction placeBlock(BlockPos pos, @Nullable FindItemResult item, int priority) {
        log.info("placeBlock({}, {})", pos, priority);

        DefaultBlockAction action = new DefaultBlockAction(pos, item, priority, Action.State.Pending);
        actions.add(action);

        return action;
    }

    @Override
    public BlockAction breakBlock(BlockPos pos, @Nullable FindItemResult item, int priority) {
        throw new NotImplementedException();
    }

    @Override
    public EntityAction interactEntity(Entity entity, @Nullable FindItemResult item, EntityInteractType interaction, int priority) {
        throw new NotImplementedException();
    }

    @Override
    public Action rotate(double yaw, double pitch, int priority) {
        throw new NotImplementedException();
    }

    // TODO: This should happen after rotations are sent, not in a post tick event
    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        actions.sort(Comparator.comparingInt(DefaultAction::getPriority));

        int blockActionCount = 0;

        for (DefaultAction action : actions) {
            if (action instanceof DefaultBlockAction blockAction) {
                if (blockActionCount++ < getBlocksPerTick()) finish(blockAction);
                else blockAction.setState(Action.State.Cancelled);
            }
        }

        actions.clear();
    }

    // TODO: Needs slot switching
    private void finish(DefaultBlockAction action) {
        Vec3d hitPos = Vec3d.ofCenter(action.getPos());

        BlockPos neighbour;
        Direction side = BlockUtils.getPlaceSide(action.getPos());

        if (side == null) {
            side = Direction.UP;
            neighbour = action.getPos();
        }
        else {
            neighbour = action.getPos().offset(side);
            hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        BlockHitResult hit = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);

        action.setState(Action.State.Finished);
    }

    // TODO: Replace with a setting
    private int getBlocksPerTick() {
        return 1;
    }
}
