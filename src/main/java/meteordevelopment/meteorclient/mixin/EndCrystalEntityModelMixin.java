/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalEntityModel.class)
public abstract class EndCrystalEntityModelMixin {
    // Chams - Bounce

    @ModifyExpressionValue(method = "setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EndCrystalEntityRenderer;getYOffset(F)F"))
    private float setAngles$bounce(float original, EndCrystalEntityRenderState state) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return original;

        float g = MathHelper.sin(state.age * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F * module.crystalsBounce.get().floatValue();
        return g - 1.4F;
    }

    // Chams - Rotation speed

    @ModifyExpressionValue(method = "setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;age:F", ordinal = 0))
    private float modifySpeed(float original) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return original;

        return original * module.crystalsRotationSpeed.get().floatValue();
    }
}
