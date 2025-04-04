/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("currentBreakingProgress")
    float getBreakingProgress();

    @Accessor("currentBreakingProgress")
    void setCurrentBreakingProgress(float progress);

    @Accessor("currentBreakingPos")
    BlockPos getCurrentBreakingBlockPos();
}
