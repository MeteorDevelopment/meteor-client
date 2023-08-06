/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightDataAccess.class, remap = false)
public class SodiumLightDataAccessMixin {
    @Unique
    private static final int FULL_LIGHT = 15 | 15 << 4 | 15 << 8;

    @Shadow
    protected BlockRenderView world;
    @Shadow @Final
    private BlockPos.Mutable pos;

    @Unique
    private Xray xray;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        xray = Modules.get().get(Xray.class);
    }

    @ModifyVariable(method = "compute", at = @At(value = "TAIL"), name = "bl")
    private int compute_modifyBL(int light) {
        if (xray.isActive()) {
            BlockState state = world.getBlockState(pos);
            if (!xray.isBlocked(state.getBlock(), pos)) return FULL_LIGHT;
        }

        return light;
    }

    @ModifyVariable(method = "compute", at = @At(value = "TAIL"), name = "sl")
    private int compute_modifySL(int light) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(), light);
    }
}
