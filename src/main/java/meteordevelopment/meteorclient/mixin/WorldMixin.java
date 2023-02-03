/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    // Player model rendering in main menu

    @Shadow @Final protected MutableWorldProperties properties;

    @Inject(at = @At(value = "HEAD"), method = "getSpawnPos", cancellable = true)
    public void getSpawnPos(CallbackInfoReturnable<BlockPos> cir) {
        if (this.properties == null) cir.setReturnValue(new BlockPos(0, 0, 0));
    }

    @Inject(at = @At(value = "HEAD"), method = "getSpawnAngle", cancellable = true)
    public void getSpawnAngle(CallbackInfoReturnable<Float> cir) {
        if (this.properties == null) cir.setReturnValue(0F);
    }
}
