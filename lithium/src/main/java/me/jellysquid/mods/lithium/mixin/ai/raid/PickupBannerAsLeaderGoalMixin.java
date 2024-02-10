package me.jellysquid.mods.lithium.mixin.ai.raid;

import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RaiderEntity.PickupBannerAsLeaderGoal.class)
public class PickupBannerAsLeaderGoalMixin {
    // The call to Raid#getOminousBanner() is very expensive, so cache it and re-use it during AI ticking
    private static final ItemStack CACHED_OMINOUS_BANNER = Raid.getOminousBanner();

    @Redirect(
            method = "canStart()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/village/raid/Raid;getOminousBanner()Lnet/minecraft/item/ItemStack;"
            )
    )
    private ItemStack getOminousBanner() {
        return CACHED_OMINOUS_BANNER;
    }
}