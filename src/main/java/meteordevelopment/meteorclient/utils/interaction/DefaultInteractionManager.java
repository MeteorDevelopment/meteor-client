/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

public class DefaultInteractionManager implements InteractionManager {
    @Override
    public BlockAction placeBlock(BlockPos pos, @Nullable FindItemResult item, int priority) {
        throw new NotImplementedException();
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
}
