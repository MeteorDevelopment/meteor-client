package me.jellysquid.mods.lithium.mixin.collections.gamerules;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GameRules.class)
public class GameRulesMixin {
    @Mutable
    @Shadow
    @Final
    private Map<GameRules.Key<?>, GameRules.Rule<?>> rules;

    @Inject(
            method = "<init>()V",
            at = @At("RETURN")
    )
    private void reinitializeMap(CallbackInfo ci) {
        this.rules = new Object2ObjectOpenHashMap<>(this.rules);
    }

    @Inject(
            method = "<init>(Ljava/util/Map;)V",
            at = @At("RETURN")
    )
    private void reinitializeMap(Map<?, ?> rules, CallbackInfo ci) {
        this.rules = new Object2ObjectOpenHashMap<>(this.rules);
    }
}
