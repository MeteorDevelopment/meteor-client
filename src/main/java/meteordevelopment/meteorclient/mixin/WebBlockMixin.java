/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.world.level.block.WebBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WebBlock.class)
public abstract class WebBlockMixin {
    // TODO(Ravel): target method method_9548 with the signature not found
// TODO(Ravel): target method onEntityCollision with the signature not found
    /*@Dynamic("Explicit 1.21.9 Support")
    @Inject(method = {
        "onEntityCollision", // 1.21.10
        "onEntityCollision(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/EntityCollisionHandler;)V", // 1.21.9 yarn
        "method_9548(Lnet/minecraft/class_2680;Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_1297;Lnet/minecraft/class_10774;)V" // 1.21.9 intermediary
    }, at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(CallbackInfo ci, @Local(argsOnly = true) Entity entity) {
        if (entity == mc.player && Modules.get().get(NoSlow.class).cobweb()) ci.cancel();
    }*/
}
