package me.jellysquid.mods.lithium.mixin.ai.task.memory_change_counting;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.lithium.common.ai.MemoryModificationCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(MultiTickTask.class)
public class MultiTickTaskMixin<E extends LivingEntity> {
    @Mutable
    @Shadow
    @Final
    protected Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryStates;

    @Unique
    private long cachedMemoryModCount = -1;
    @Unique
    private boolean cachedHasRequiredMemoryState;

    @Inject(method = "<init>(Ljava/util/Map;II)V", at = @At("RETURN"))
    private void init(Map<MemoryModuleType<?>, MemoryModuleState> map, int int_1, int int_2, CallbackInfo ci) {
        this.requiredMemoryStates = new Reference2ObjectOpenHashMap<>(map);
    }

    /**
     * @reason Use cached required memory state test result if memory state is unchanged
     * @author 2No2Name
     */
    @Overwrite
    public boolean hasRequiredMemoryState(E entity) {
        Brain<?> brain = entity.getBrain();
        long modCount = ((MemoryModificationCounter) brain).getModCount();
        if (this.cachedMemoryModCount == modCount) {
            return this.cachedHasRequiredMemoryState;
        }
        this.cachedMemoryModCount = modCount;

        ObjectIterator<Reference2ObjectMap.Entry<MemoryModuleType<?>, MemoryModuleState>> fastIterator = ((Reference2ObjectOpenHashMap<MemoryModuleType<?>, MemoryModuleState>) this.requiredMemoryStates).reference2ObjectEntrySet().fastIterator();
        while (fastIterator.hasNext()) {
            Reference2ObjectMap.Entry<MemoryModuleType<?>, MemoryModuleState> entry = fastIterator.next();
            if (!brain.isMemoryInState(entry.getKey(), entry.getValue())) {
                return this.cachedHasRequiredMemoryState = false;
            }
        }

        return this.cachedHasRequiredMemoryState = true;
    }
}
