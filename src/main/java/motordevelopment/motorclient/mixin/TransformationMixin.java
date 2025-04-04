/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.render.ApplyTransformationEvent;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Transformation.class)
public abstract class TransformationMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void onApply(boolean leftHanded, MatrixStack matrices, CallbackInfo info) {
        ApplyTransformationEvent event = MotorClient.EVENT_BUS.post(ApplyTransformationEvent.get((Transformation) (Object) this, leftHanded, matrices));
        if (event.isCancelled()) info.cancel();
    }
}
