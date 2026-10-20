/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.mc.MixinTargets;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Overrides the swing duration for the local player.
 *
 * <p>26.2 computes it in {@code getCurrentSwingDuration}, 26.3 and 26.4 in
 * {@code getModifiedSwingDuration}.
 */
@Mixin(LivingEntity.class)
public abstract class SwingDurationMixin {
    @ModifyExpressionValue(method = MixinTargets.SWING_DURATION_METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I"))
    private int getHandSwingDuration(int original) {
        if ((Object) this != mc.player) return original;

        return Modules.get().get(HandView.class).isActive() && mc.options.getCameraType().isFirstPerson() ? Modules.get().get(HandView.class).swingSpeed.get() : original;
    }
}
