/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public interface ILivingEntityRenderer {
    void setupTransformsInterface(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta);
    void scaleInterface(LivingEntity entity, MatrixStack matrices, float amount);
    boolean isVisibleInterface(LivingEntity entity);
    float getAnimationCounterInterface(LivingEntity entity, float tickDelta);
}
