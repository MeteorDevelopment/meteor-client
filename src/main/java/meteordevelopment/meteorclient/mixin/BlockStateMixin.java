/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.BlockActivateEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class BlockStateMixin extends BlockBehaviour.BlockStateBase {
    protected BlockStateMixin(Block owner, Property<?>[] propertyKeys, Comparable<?>[] propertyValues) {
        super(owner, propertyKeys, propertyValues);
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull Level level, @NonNull Player player, @NonNull BlockHitResult hitResult) {
        MeteorClient.EVENT_BUS.post(BlockActivateEvent.get((BlockState) (Object) this));
        return super.useWithoutItem(level, player, hitResult);
    }
}
