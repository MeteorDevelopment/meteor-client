/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.particle.FireworkParticles;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireworkParticles.Starter.class)
public abstract class FireworksSparkParticleMixin {
}
