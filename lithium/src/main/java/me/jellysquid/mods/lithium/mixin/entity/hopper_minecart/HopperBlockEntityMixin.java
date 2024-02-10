package me.jellysquid.mods.lithium.mixin.entity.hopper_minecart;

import me.jellysquid.mods.lithium.common.world.ItemEntityHelper;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collections;
import java.util.List;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    /**
     * @author 2No2Name
     * @reason avoid checking 5 boxes
     * <p>
     * This code is run by hopper minecarts. Hopper blocks use a different optimization unless it is disabled.
     */
    @Overwrite
    public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
        Box encompassingBox = hopper.getInputAreaShape().getBoundingBox();
        double xOffset = hopper.getHopperX() - 0.5;
        double yOffset = hopper.getHopperY() - 0.5;
        double zOffset = hopper.getHopperZ() - 0.5;
        List<ItemEntity> nearbyEntities = world.getEntitiesByClass(ItemEntity.class, encompassingBox.offset(xOffset, yOffset, zOffset), EntityPredicates.VALID_ENTITY);

        if (nearbyEntities.isEmpty()) {
            return Collections.emptyList();
        }

        return ItemEntityHelper.getItemEntityBucketedList(hopper, xOffset, yOffset, zOffset, nearbyEntities);
    }
}
