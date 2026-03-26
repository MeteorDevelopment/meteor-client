/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ChangePerspectiveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Options.class)
public abstract class GameOptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void onInitAfterKeysAll(Minecraft client, File optionsFile, CallbackInfo info) {
        keyMappings = KeyBinds.apply(keyMappings);
    }

    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
    private void setPerspective(CameraType perspective, CallbackInfo info) {
        if (Modules.get() == null) return; // nothing is loaded yet, shouldersurfing compat

        ChangePerspectiveEvent event = MeteorClient.EVENT_BUS.post(ChangePerspectiveEvent.get(perspective));

        if (event.isCancelled()) info.cancel();

        if (Modules.get().isActive(Freecam.class)) info.cancel();
    }
}
