/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.impl;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.interaction.api.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
    private final Config config;

    public DefaultInteractionManager() {
        config = Config.get();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

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

    // TODO: This should happen after rotations are sent, not in a post tick event
    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        actions.sort(Comparator.comparingInt(DefaultAction::getPriority));

        int blockActionCount = 0;

        for (DefaultAction action : actions) {
            if (action instanceof DefaultBlockAction blockAction) {
                if (blockActionCount < config.placementsPerTick.get()) {
                    if (place(blockAction)) blockActionCount++;
                }
                else blockAction.setState(Action.State.Cancelled);
            }
        }

        actions.clear();
    }

    // TODO: Needs slot switching
    private boolean place(DefaultBlockAction action) {
        Vec3d hitPos = Vec3d.ofCenter(action.getPos());

        BlockPos neighbour;
        Direction side = getPlaceSide(action.getPos());

        if (side == null) {
            if (!config.airPlace.get()) {
                action.setState(Action.State.Cancelled);
                return false;
            }

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
        return true;
    }

    public Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || BlockUtils.isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            // ensure you're placing against a side you can see
            if (config.validBlockSide.get()) {
                Vec3d vec = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
                Box blockBox = new Box(blockPos);

                switch (side) {
                    case UP -> {
                        if (vec.y > blockBox.maxY) continue;
                    }

                    case DOWN -> {
                        if (vec.y < blockBox.minY) continue;
                    }

                    case NORTH -> {
                        if (vec.z < blockBox.minZ) continue;
                    }

                    case SOUTH -> {
                        if (vec.z > blockBox.maxZ) continue;
                    }

                    case EAST -> {
                        if (vec.x > blockBox.maxX) continue;
                    }

                    case WEST -> {
                        if (vec.x < blockBox.minX) continue;
                    }
                }
            }

            return side;
        }

        return null;
    }

    // Rotations
    // todo change everything to use rotations from here

    // only one rotation per tick
    private Rotation currentRotation;

    public float serverYaw;
    public float serverPitch;
    public int rotationTimer;
    private float preYaw, prePitch;

    public boolean rotating = false;

    @Override
    public Action rotate(double yaw, double pitch, int priority) {
        log.info("new rotation: {} {}", yaw, pitch);
        Rotation rotation = new Rotation();
        rotation.set(yaw, pitch, priority);
        rotation.action = new DefaultAction(priority, null);

        if (currentRotation == null || currentRotation.action.getState() == Action.State.Finished || rotation.priority > currentRotation.priority) {
            if (currentRotation != null) currentRotation.action.setState(currentRotation.action.getState() == null ? Action.State.Cancelled : Action.State.Finished);

            currentRotation = rotation;
        } else {
            rotation.action.setState(Action.State.Cancelled);
        }

        return rotation.action;
    }

    @EventHandler
    private void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
        if (mc.cameraEntity != mc.player) return;

        if (currentRotation != null) {
            rotating = true;

            if (currentRotation.action.getState() == null) currentRotation.action.setState(Action.State.Pending);

            if (rotationTimer >= config.rotationHoldTicks.get()) {
                currentRotation = null;
                rotating = false;
                rotationTimer = 0;

                return;
            }

            setupMovementPacketRotation(currentRotation);
        }
    }

    // we don't need to send any packets ourselves, it should all be handled in ClientPlayerEntity::sendMovementPackets
    // and sending packets when we don't need to will flag
    @EventHandler
    private void onSendMovementPacketsPost(SendMovementPacketsEvent.Post event) {
        if (currentRotation == null) return;

        if (currentRotation.action.getState() == Action.State.Pending) {
            if (mc.cameraEntity == mc.player) resetPreRotation();

            currentRotation.action.setState(Action.State.Finished);
        } else if (currentRotation.action.getState() == Action.State.Finished) {
            resetPreRotation();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (currentRotation != null) rotationTimer++;
    }

    private void setupMovementPacketRotation(Rotation rotation) {
        setClientRotation(rotation);
        setCamRotation(rotation.yaw, rotation.pitch);
    }

    private void setClientRotation(Rotation rotation) {
        preYaw = mc.player.getYaw();
        prePitch = mc.player.getPitch();

        mc.player.setYaw((float) rotation.yaw);
        mc.player.setPitch((float) rotation.pitch);
    }

    public void setCamRotation(double yaw, double pitch) {
        serverYaw = (float) yaw;
        serverPitch = (float) pitch;
    }

    private void resetPreRotation() {
        mc.player.setYaw(preYaw);
        mc.player.setPitch(prePitch);
    }

    private static class Rotation {
        public double yaw, pitch;
        public int priority;
        public DefaultAction action;

        public void set(double yaw, double pitch, int priority) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
        }
    }
}
