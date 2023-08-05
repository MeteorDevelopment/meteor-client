/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightDataAccess.class, remap = false)
public abstract class LightDataAccessMixin {
    @Unique private static final int FULL_LIGHT = 15 | 15 << 4 | 15 << 8;

    @Shadow protected BlockRenderView world;
    @Shadow @Final private BlockPos.Mutable pos;

    @Shadow public static int packBL(int blockLight) { return 0; }
    @Shadow public static int packSL(int skyLight) { return 0; }

    @Unique
    private Xray xray;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        xray = Modules.get().get(Xray.class);
    }

    @Redirect(method = "compute", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/model/light/data/LightDataAccess;packBL(I)I"))
    private int onPackBL(int blockLight) {
        if (xray.isActive()) {
            BlockState state = world.getBlockState(pos);
            if (!xray.isBlocked(state.getBlock(), pos)) return FULL_LIGHT;
        }
        return packBL(blockLight);
    }

    @ModifyArg(method = "compute", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/model/light/data/LightDataAccess;packSL(I)I"))
    private int onPackSL(int blockLight) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(), blockLight);
    }
}
