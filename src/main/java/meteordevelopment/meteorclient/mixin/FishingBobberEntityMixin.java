/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IFishingBobberEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin implements IFishingBobberEntity {
    @Shadow protected abstract boolean isOpenOrWaterAround(BlockPos pos);

    @Override
    public boolean inOpenWater(BlockPos pos) {
        return isOpenOrWaterAround(pos);
    }
}
