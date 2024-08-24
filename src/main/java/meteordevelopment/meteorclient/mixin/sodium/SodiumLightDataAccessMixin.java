/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightDataAccess.class, remap = false)
public abstract class SodiumLightDataAccessMixin {
    @Unique
    private static final int FULL_LIGHT = 15 | 15 << 4 | 15 << 8;

    @Shadow
    protected BlockRenderView level;
    @Shadow @Final
    private BlockPos.Mutable pos;

    @Unique
    private Xray xray;

    @Unique
    private Fullbright fb;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        xray = Modules.get().get(Xray.class);
        fb = Modules.get().get(Fullbright.class);
    }

    @ModifyVariable(method = "compute", at = @At(value = "TAIL"), name = "bl")
    private int compute_modifyBL(int light) {
        if (xray.isActive()) {
            BlockState state = level.getBlockState(pos);
            if (!xray.isBlocked(state.getBlock(), pos)) return FULL_LIGHT;
        }

        return light;
    }

    // fullbright

    @ModifyVariable(method = "compute", at = @At(value = "STORE"), name = "sl")
    private int compute_assignSL(int sl) {
        return Math.max(fb.getLuminance(LightType.SKY), sl);
    }

    @ModifyVariable(method = "compute", at = @At(value = "STORE"), name = "bl")
    private int compute_assignBL(int bl) {
        return Math.max(fb.getLuminance(LightType.BLOCK), bl);
    }
}
