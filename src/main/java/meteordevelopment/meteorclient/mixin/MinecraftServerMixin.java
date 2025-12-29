/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import java.io.File;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.TickManipulator;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.server.MinecraftServer;

import net.fabricmc.loader.impl.game.minecraft.Hooks;

import org.jetbrains.annotations.Nullable;
import org.meteordev.starscript.Script;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
{
    	// require = 0 to support quilt
   	@Redirect(method = "<init>", require = 0, at = @At(value = "INVOKE", target = "Lnet/fabricmc/loader/impl/game/minecraft/Hooks;startServer(Ljava/io/File;Ljava/lang/Object;)V", remap = false))
    private void catchFabricInit(File runDir, Object gameInstance)
	{
		if(Modules.get() != null)
		{
			try
	    	{
				Hooks.startServer(runDir, gameInstance);
            } 
			catch (Throwable throwable)
			{}
		}
		else
		{
			Hooks.startServer(runDir, gameInstance);
		}
	}

    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    private void work(CallbackInfo ci)
    {
        if(Modules.get().get(TickManipulator.class).serverTime())
        ci.cancel();
    }
}
