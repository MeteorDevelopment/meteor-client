/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.SoundBlocker;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundSystemMixin {
    @Shadow
    public abstract void stop(SoundInstance sound);

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PlaySoundEvent event = MeteorClient.EVENT_BUS.post(PlaySoundEvent.get(soundInstance));

        if (event.isCancelled()) cir.cancel();
    }

    @Inject(method = "tickInGameSound()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/TickableSoundInstance;tick()V", ordinal = 0))
    private void onTick(CallbackInfo ci, @Local TickableSoundInstance tickableSoundInstance) {
        if (Modules.get().get(SoundBlocker.class).shouldBlock(tickableSoundInstance)) stop(tickableSoundInstance);
    }
}
