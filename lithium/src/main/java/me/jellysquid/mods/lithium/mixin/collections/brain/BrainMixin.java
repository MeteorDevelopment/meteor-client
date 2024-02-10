package me.jellysquid.mods.lithium.mixin.collections.brain;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.entity.ai.brain.Brain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(Brain.class)
public class BrainMixin {

    @Mutable
    @Shadow
    @Final
    private Map<?, ?> memories;

    @Mutable
    @Shadow
    @Final
    private Map<?, ?> sensors;

    @Inject(
            method = "<init>(Ljava/util/Collection;Ljava/util/Collection;Lcom/google/common/collect/ImmutableList;Ljava/util/function/Supplier;)V",
            at = @At("RETURN")
    )
    private void reinitializeBrainCollections(Collection<?> memories, Collection<?> sensors, ImmutableList<?> memoryEntries, Supplier<?> codecSupplier, CallbackInfo ci) {
        this.memories = new Reference2ReferenceOpenHashMap<>(this.memories);
        this.sensors = new Reference2ReferenceLinkedOpenHashMap<>(this.sensors);
    }

}
