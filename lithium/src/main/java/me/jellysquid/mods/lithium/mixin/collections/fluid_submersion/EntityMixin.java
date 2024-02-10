package me.jellysquid.mods.lithium.mixin.collections.fluid_submersion;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Entity.class)
public class EntityMixin {

    @Mutable
    @Shadow
    @Final
    private Set<TagKey<Fluid>> submergedFluidTag;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void useReferenceArraySet(CallbackInfo ci) {
        this.submergedFluidTag = new ReferenceArraySet<>(this.submergedFluidTag);
    }

    @Redirect(
            method = "updateSubmergedInWaterState()V",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"),
            require = 0
    )
    private void clearIfNotEmpty(Set<?> set) {
        if (!set.isEmpty()) {
            set.clear();
        }
    }
}
