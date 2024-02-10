package me.jellysquid.mods.lithium.mixin.ai.raid;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
public abstract class RaidMixin {
    @Shadow
    @Final
    private ServerBossBar bar;

    @Shadow
    public abstract float getCurrentRaiderHealth();

    @Shadow
    private float totalHealth;

    private boolean isBarDirty;

    /**
     * Check if an update was queued for the bar, and if so, perform an update
     */
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.isBarDirty) {
            this.bar.setPercent(MathHelper.clamp(this.getCurrentRaiderHealth() / this.totalHealth, 0.0F, 1.0F));

            this.isBarDirty = false;
        }
    }

    /**
     * @reason Delay re-calculating and sending progress bar updates until the next tick to avoid excessive updates
     * @author JellySquid
     */
    @Overwrite
    public void updateBar() {
        this.isBarDirty = true;
    }

}
