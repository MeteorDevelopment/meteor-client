package me.jellysquid.mods.lithium.common.world;

import me.jellysquid.mods.lithium.common.entity.pushable.BlockCachingEntity;
import me.jellysquid.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;

public interface ClimbingMobCachingSection {

    LazyIterationConsumer.NextIteration collectPushableEntities(World world, Entity except, Box box, EntityPushablePredicate<? super Entity> entityPushablePredicate, ArrayList<Entity> entities);

    void onEntityModifiedCachedBlock(BlockCachingEntity entity, BlockState newBlockState);
}
