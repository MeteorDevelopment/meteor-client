package me.jellysquid.mods.lithium.mixin.collections.attributes;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(AttributeContainer.class)
public class AttributeContainerMixin {
    @Mutable
    @Shadow
    @Final
    private Map<EntityAttribute, EntityAttributeInstance> custom;

    @Mutable
    @Shadow
    @Final
    private Set<EntityAttributeInstance> tracked;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void initCollections(DefaultAttributeContainer defaultAttributes, CallbackInfo ci) {
        this.custom = new Reference2ReferenceOpenHashMap<>(0);
        this.tracked = new ReferenceOpenHashSet<>(0);
    }
}
