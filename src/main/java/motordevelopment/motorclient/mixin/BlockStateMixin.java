/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.world.BlockActivateEvent;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public abstract class BlockStateMixin extends AbstractBlock.AbstractBlockState {
    public BlockStateMixin(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<BlockState> mapCodec) {
        super(block, propertyMap, mapCodec);
    }

    @Override
    public ActionResult onUse(World world, PlayerEntity player, BlockHitResult hit) {
        MotorClient.EVENT_BUS.post(BlockActivateEvent.get((BlockState) (Object) this));
        return super.onUse(world, player, hit);
    }
}
