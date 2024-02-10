package me.jellysquid.mods.lithium.mixin.util.entity_section_position;

import me.jellysquid.mods.lithium.common.entity.PositionedEntityTrackingSection;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionedEntityCache.class)
public class SectionedEntityCacheMixin<T extends EntityLike> {
    @Inject(method = "addSection(J)Lnet/minecraft/world/entity/EntityTrackingSection;", at = @At("RETURN"))
    private void rememberPos(long sectionPos, CallbackInfoReturnable<EntityTrackingSection<T>> cir) {
        ((PositionedEntityTrackingSection) cir.getReturnValue()).setPos(sectionPos);
    }
}
