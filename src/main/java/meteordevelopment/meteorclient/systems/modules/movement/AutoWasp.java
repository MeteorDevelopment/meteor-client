/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.mixin.DirectionAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class AutoWasp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> horizontalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal elytra speed.")
        .defaultValue(2.0)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical elytra speed.")
        .defaultValue(3.0)
        .build()
    );

    private final Setting<Boolean> avoidLanding = sgGeneral.add(new BoolSetting.Builder()
        .name("avoid-landing")
        .description("Will try to avoid landing if your target is on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Tries to predict the targets position according to their movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("only-friends")
        .description("Will only follow friends.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Action> action = sgGeneral.add(new EnumSetting.Builder<Action>()
        .name("action-on-target-loss")
        .description("What to do if you lose the target.")
        .defaultValue(Action.TOGGLE)
        .build()
    );

    private final Setting<Vector3d> offset = sgGeneral.add(new Vector3dSetting.Builder()
        .name("offset")
        .description("How many blocks offset to wasp at from the target.")
        .defaultValue(0, 0, 0)
        .build()
    );

    public Player target;
    private int jumpTimer = 0;
    private boolean incrementJumpTimer = false;

    public AutoWasp() {
        super(Categories.Movement, "auto-wasp", "Wasps for you. Unable to traverse around blocks, assumes a clear straight line to the target.");
    }

    @Override
    public void onActivate() {
        if (target == null || target.isRemoved()) {
            target = (Player) TargetUtils.get(entity -> {
                if (!(entity instanceof Player player) || entity == mc.player) return false;
                if (player.isDeadOrDying() || player.getHealth() <= 0) return false;
                return !onlyFriends.get() || Friends.get().get(player) != null;
            }, SortPriority.LowestDistance);

            if (target == null) {
                error("No valid targets.");
                toggle();
                return;
            } else info(target.getName().getString() + " set as target.");
        }

        jumpTimer = 0;
        incrementJumpTimer = false;
    }

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (target.isRemoved()) {
            warning("Lost target!");

            switch (action.get()) {
                case CHOOSE_NEW_TARGET -> onActivate();
                case TOGGLE -> toggle();
                case DISCONNECT ->
                    mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal("%s[%sAuto Wasp%s] Lost target.".formatted(ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.GRAY))));
            }

            if (!isActive()) return;
        }

        if (!(mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER))) return;

        if (incrementJumpTimer) {
            jumpTimer++;
        }

        if (!mc.player.isFallFlying()) {
            if (!incrementJumpTimer) incrementJumpTimer = true;

            if (mc.player.onGround() && incrementJumpTimer) {
                mc.player.jumpFromGround();
                return;
            }

            if (jumpTimer >= 4) {
                jumpTimer = 0;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } else {
            incrementJumpTimer = false;
            jumpTimer = 0;
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (!(mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER))) return;
        if (!mc.player.isFallFlying()) return;

        double xVel = 0, yVel = 0, zVel = 0;

        Vec3 targetPos = target.position().add(offset.get().x, offset.get().y, offset.get().z);

        if (predictMovement.get()) targetPos.add(Player.collideBoundingBox(target, target.getDeltaMovement(),
            target.getBoundingBox(), mc.level, mc.level.getEntityCollisions(target, target.getBoundingBox().expandTowards(target.getDeltaMovement()))));

        if (avoidLanding.get()) {
            double d = target.getBoundingBox().getXsize() / 2; // x length = z length for players

            //get the block pos of the block underneath the corner of the targets bounding box
            for (Direction dir : DirectionAccessor.meteor$getHorizontal()) {
                BlockPos pos = BlockPos.containing(targetPos.relative(dir, d).relative(dir.getClockWise(), d)).below();
                if (((AbstractBlockAccessor) mc.level.getBlockState(pos).getBlock()).meteor$isCollidable() && Math.abs(targetPos.y() - (pos.getY() + 1)) <= 0.25) {
                    targetPos = new Vec3(targetPos.x, pos.getY() + 1.25, targetPos.z);
                    break;
                }
            }
        }

        double xDist = targetPos.x() - mc.player.getX();
        double zDist = targetPos.z() - mc.player.getZ();

        double absX = Math.abs(xDist);
        double absZ = Math.abs(zDist);

        double diag = 0;
        if (absX > 1.0E-5F && absZ > 1.0E-5F) diag = 1 / Math.sqrt(absX * absX + absZ * absZ);

        if (absX > 1.0E-5F) {
            if (absX < horizontalSpeed.get()) xVel = xDist;
            else xVel = horizontalSpeed.get() * Math.signum(xDist);

            if (diag != 0) xVel *= (absX * diag);
        }

        if (absZ > 1.0E-5F) {
            if (absZ < horizontalSpeed.get()) zVel = zDist;
            else zVel = horizontalSpeed.get() * Math.signum(zDist);

            if (diag != 0) zVel *= (absZ * diag);
        }

        double yDist = targetPos.y() - mc.player.getY();
        if (Math.abs(yDist) > 1.0E-5F) {
            if (Math.abs(yDist) < verticalSpeed.get()) yVel = yDist;
            else yVel = verticalSpeed.get() * Math.signum(yDist);
        }

        ((IVec3d) event.movement).meteor$set(xVel, yVel, zVel);
    }

    public enum Action {
        TOGGLE,
        CHOOSE_NEW_TARGET,
        DISCONNECT;

        @Override
        public String toString() {
            return switch (this) {
                case TOGGLE -> "Toggle module";
                case CHOOSE_NEW_TARGET -> "Choose new target";
                case DISCONNECT -> "Disconnect";
            };
        }
    }
}
