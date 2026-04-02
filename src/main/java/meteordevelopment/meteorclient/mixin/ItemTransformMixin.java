/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemTransform.class)
public abstract class ItemTransformMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void onApply(boolean applyLeftHandFix, PoseStack.Pose pose, CallbackInfo ci) {
        ApplyTransformationEvent event = MeteorClient.EVENT_BUS.post(ApplyTransformationEvent.get((ItemTransform) (Object) this, applyLeftHandFix));
        if (event.isCancelled()) ci.cancel();
    }
}
