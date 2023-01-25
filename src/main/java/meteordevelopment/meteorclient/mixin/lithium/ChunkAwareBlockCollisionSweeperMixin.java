/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.lithium;

import com.google.common.collect.AbstractIterator;
import me.jellysquid.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChunkAwareBlockCollisionSweeper.class, remap = false)
public abstract class ChunkAwareBlockCollisionSweeperMixin extends AbstractIterator<VoxelShape> {
    @Shadow private int cIterated;
    @Shadow private int cTotalSize;
    @Shadow protected abstract boolean nextSection();
    @Shadow private int cX;
    @Shadow private int cY;
    @Shadow private int cZ;
    @Shadow private int cEndX;
    @Shadow private int cEndZ;
    @Shadow private int cStartX;
    @Shadow private int cStartZ;
    @Shadow private boolean sectionOversizedBlocks;
    @Shadow @Final private int minX;
    @Shadow @Final private int maxX;
    @Shadow @Final private int minY;
    @Shadow private ChunkSection cachedChunkSection;
    @Shadow @Final private int maxY;
    @Shadow @Final private int minZ;
    @Shadow @Final private int maxZ;
    @Shadow @Final private CollisionView view;
    @Shadow @Final private BlockPos.Mutable pos;
    @Shadow @Final private ShapeContext context;
    @Shadow @Final private Box box;
    @Shadow @Final private VoxelShape shape;

    @Shadow
    private static boolean canInteractWithBlock(BlockState state, int edgesHit) {
        return false;
    }
    @Shadow
    private static VoxelShape getCollidedShape(Box entityBox, VoxelShape entityShape, VoxelShape shape, int x, int y, int z) {
        return null;
    }

    // @Redirect crashes the game somehow
    @Override
    public VoxelShape computeNext() {
        while(cIterated < cTotalSize || nextSection()) {
            ++cIterated;
            int x = cX;
            int y = cY;
            int z = cZ;
            if (cX < cEndX) {
                ++cX;
            } else if (cZ < cEndZ) {
                cX = cStartX;
                ++cZ;
            } else {
                cX = cStartX;
                cZ = cStartZ;
                ++cY;
            }

            int edgesHit = sectionOversizedBlocks ? (x >= minX && x <= maxX ? 0 : 1) + (y >= minY && y <= maxY ? 0 : 1) + (z >= minZ && z <= maxZ ? 0 : 1) : 0;
            if (edgesHit != 3) {
                BlockState state = cachedChunkSection.getBlockState(x & 15, y & 15, z & 15);
                if (canInteractWithBlock(state, edgesHit)) {
                    this.pos.set(x, y, z);
                    CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(state, pos, state.getCollisionShape(view, pos, context)));
                    VoxelShape collisionShape = event.isCancelled() ? VoxelShapes.empty() : event.shape;
                    if (collisionShape != VoxelShapes.empty()) {
                        VoxelShape collidedShape = getCollidedShape(box, shape, collisionShape, x, y, z);
                        if (collidedShape != null) {
                            return collidedShape;
                        }
                    }
                }
            }
        }

        return this.endOfData();
    }
}
