/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.api;

import meteordevelopment.meteorclient.utils.interaction.RotationUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public interface InteractionManager {
    class Priority {
        public static final int HIGHEST = 200;
        public static final int HIGH = 100;
        public static final int NORMAL = 0;
        public static final int LOW = -100;
        public static final int LOWEST = -200;
    }

    /** Needs to be called from TickEvent.Pre and then modules can check the state in TickEvent.Post.
     *  If a FindItemResult is passed, the client will swap to that item before performing the interaction.
     *  All interactions that can't be executed in the current tick will be cancelled. */
    BlockAction placeBlock(BlockPos pos, @Nullable FindItemResult item, int priority);

    /** Needs to be called from TickEvent.Pre and then modules can check the state in TickEvent.Post.
     *  If a FindItemResult is passed, the client will swap to that item before performing the interaction.
     *  Modules are supposed to call this method every TickEvent.Pre, if they don't then it will cancel the block
     *  breaking. If one module is currently in the middle of breaking a block with priority 5 and another module
     *  starts breaking a block with priority, the manager will cancel the interaction with priority 5. The Interaction
     *  object returned by this method will be the same between all the ticks a module continues to break a block at the
     *  same location. The Pending state will be reported all the way until the block is either broken or an interaction
     *  has been cancelled by some other, higher priority interaction. */
    BlockAction breakBlock(BlockPos pos, @Nullable FindItemResult item, int priority);

    /** Needs to be called from TickEvent.Pre and then modules can check the state in TickEvent.Post.
     *  If a FindItemResult is passed, the client will swap to that item before performing the interaction. */
    EntityAction interactEntity(Entity entity, @Nullable FindItemResult item, EntityInteractType interaction, int priority);

    /** Needs to be called from TickEvent.Pre and then the modules can check the state in TickEvent.Post.
     * The Cancelled state will be returned for the rotation with lower priority if two separate modules send a
     * rotation in the same tick.
     * The Pending state will be returned before the rotation packet has been sent.
     * The Finished state will be returned after the rotation packet has been sent. */
    Action rotate(double yaw, double pitch, int priority);

    default Action rotate(double yaw, double pitch) {
        return rotate(yaw, pitch, Priority.NORMAL);
    }

    default Action rotate(Entity entity) {
        return rotate(RotationUtils.getYaw(entity), RotationUtils.getPitch(entity));
    }

    default Action rotate(Vec3d vec) {
        return rotate(RotationUtils.getYaw(vec), RotationUtils.getPitch(vec));
    }

    default Action rotate(BlockPos pos) {
        return rotate(RotationUtils.getYaw(pos), RotationUtils.getPitch(pos));
    }
}
