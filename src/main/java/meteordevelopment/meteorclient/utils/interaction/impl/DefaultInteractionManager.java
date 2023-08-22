/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.impl;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.interaction.RotationUtils;
import meteordevelopment.meteorclient.utils.interaction.api.*;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
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
    private DefaultAction rotateAction;
    private final Config config;
    private Vec3d lastHitPos;
    private Vec3d lastPlayerPos;

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


   /*   interaction manager tick structure
    *
    *   --- TICK START ---
    *
    *   TickEvent.Pre:
    *       - interaction manager receives interaction requests from modules
    *       - sorts them by priority
    *       - if rotations aren't enabled, place as many blocks as determined by placementsPerTick
    *       - if rotations are enabled, interact with whatever we rotated to the previous tick
    *       - cancel remaining interactions
    *
    *   SendMovementPacketsEvent
    *       - determine where to rotate based on priority
    *
    *   TickEvent.Post
    *       - modules can check the state of their pending actions
    *
    *   --- TICK END ---
    *
    *   need to keep in mind, if rotations are disabled we can place blocks as soon as we receive the requests to do so,
    *   while if rotations are enabled we can place at most one block per tick, and there is a necessary tick of delay
    *   since we need to rotate towards where we're placing
    */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onTickPre(TickEvent.Pre event) {
        actions.sort(Comparator.comparingInt(DefaultAction::getPriority));

        int blockActionCount = 0;

        if (config.rotate.get()) { // if rotate is enabled, only one action is really possible per tick
            if (rotateAction instanceof DefaultBlockAction blockAction) {
                if (blockAction.rotated || (mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getBlockPos().equals(blockAction.getPos()))) {
                    place(blockAction);
                }
            }

            rotateAction = null;

            if (!actions.isEmpty()) {
                for (DefaultAction action : actions) {
                    if (!(action instanceof DefaultBlockAction blockAction)) continue;
                    if ((blockAction.bhr = getBHR(blockAction)) == null) continue;

                    rotateAction = blockAction;
                }
            }
        }
        else {
            for (DefaultAction action : actions) {
                if (action instanceof DefaultBlockAction blockAction) {
                    if (blockActionCount < config.placementsPerTick.get()) {
                        blockAction.bhr = getBHR(blockAction);
                        if (place(blockAction)) blockActionCount++;
                    }
                    else blockAction.setState(Action.State.Cancelled);
                }
            }
        }

        actions.clear();
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (lastHitPos == null || lastPlayerPos == null) return;
        event.renderer.line(lastHitPos.x, lastHitPos.y, lastHitPos.z, lastPlayerPos.x, lastPlayerPos.y, lastPlayerPos.z, Color.WHITE);
    }


    // TODO: offhand placing
    private boolean place(DefaultBlockAction action) {
        if (action.bhr == null) return false;

        lastHitPos = action.bhr.getPos();
        lastPlayerPos = mc.player.getEyePos();

        if (action.item != null && action.item.isHotbar()) InvUtils.swap(action.item.slot(), false);
        /*  todo:
         *      investigate using inventory bypass - InvUtils.quickMove().from(invSlot).to(mc.player.getInventory().selectedSlot)
         *      silent swapping vs normal swapping
         *      swap back
         *
         *  probably don't want to end up having a ton of arguments in the method again, like BlockUtils.place, so we
         *  will have to do something to get around it - global config settings, or a record the module can pass in with
         *  the setting configuration?
         */

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, action.bhr);
        action.setState(Action.State.Finished);
        return true;
    }

    public BlockHitResult getBHR(DefaultBlockAction action) {
        Vec3d hitPos;
        BlockPos neighbour;
        Direction side = getPlaceSide(action.getPos());

        if (side == null) {
            if (!config.airPlace.get()) {
                action.setState(Action.State.Cancelled);
                return null;
            }

            side = Direction.UP;
            neighbour = action.getPos();
            hitPos = Vec3d.ofCenter(action.getPos());
        }
        else {
            neighbour = action.getPos().offset(side);

            VoxelShape shape = mc.world.getBlockState(neighbour).getOutlineShape(mc.world, neighbour);
            if (shape.isEmpty()) shape = VoxelShapes.fullCube();
            Box box = shape.getBoundingBox();

            // getting the centre of the block
            hitPos = Vec3d.of(neighbour).add(0.5, box.getYLength() * 0.5, 0.5).add(mc.world.getBlockState(neighbour).getModelOffset(mc.world, neighbour));

            //  todo there are some blocks with ingame models that dont match the outline shape (e.g. small drip leaf),
            //   which means the calculations with getModelOffset dont work.

            // setting the crosshair to the centre of the relevant side
            hitPos = hitPos.subtract(box.getXLength() * side.getOffsetX() * 0.5, box.getYLength() * side.getOffsetY() * 0.5, box.getZLength() * side.getOffsetZ() * 0.5);
        }

        return new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
    }

    public Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || BlockUtils.isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            // ensure you're placing against a logical block side
            if (config.strictSides.get()) {
                Box blockBox = new Box(blockPos);

                if (switch (side) {
                    case UP -> mc.player.getEyeY() > blockBox.maxY;
                    case DOWN -> mc.player.getEyeY() < blockBox.minY;
                    case NORTH -> mc.player.getZ() < blockBox.minZ;
                    case SOUTH -> mc.player.getZ() > blockBox.maxZ;
                    case EAST -> mc.player.getX() > blockBox.maxX;
                    case WEST -> mc.player.getX() < blockBox.minX;
                }) continue;
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
        rotation.set(yaw, pitch, priority, new DefaultAction(priority, null));

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

        if (rotateAction != null && (currentRotation == null || currentRotation.priority < rotateAction.getPriority())) {
            if (rotateAction instanceof DefaultBlockAction blockAction && blockAction.bhr != null) {
                currentRotation = new Rotation().set(
                    RotationUtils.getYaw(blockAction.bhr.getPos()),
                    RotationUtils.getPitch(blockAction.bhr.getPos()),
                    blockAction.getPriority(),
                    rotateAction
                );
            }
        }

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
            if (currentRotation.action instanceof DefaultBlockAction blockAction) blockAction.rotated = true;
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

        public Rotation set(double yaw, double pitch, int priority, DefaultAction action) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.action = action;

            return this;
        }
    }
}
