package me.jellysquid.mods.lithium.mixin.entity.collisions.intersection;

import me.jellysquid.mods.lithium.common.entity.LithiumEntityCollisions;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Replaces collision testing methods with jumps to our own (faster) entity collision testing code.
 */
@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess {

    /**
     * Checks whether the area is empty from blocks, hard entities and the world border.
     * Only access relevant entity classes, use more efficient block access
     * @author 2No2Name
     */
    @Override
    public boolean isSpaceEmpty(@Nullable Entity entity, Box box) {
        boolean ret = !LithiumEntityCollisions.doesBoxCollideWithBlocks((World) (Object) this, entity, box);

        // If no blocks were collided with, try to check for entity collisions if we can read entities
        if (ret && this instanceof EntityView) {
            //needs to include world border collision
            ret = !LithiumEntityCollisions.doesBoxCollideWithHardEntities(this, entity, box);
        }

        if (ret && entity != null) {
            ret = !LithiumEntityCollisions.doesEntityCollideWithWorldBorder(this, entity);
        }

        return ret;
    }
}