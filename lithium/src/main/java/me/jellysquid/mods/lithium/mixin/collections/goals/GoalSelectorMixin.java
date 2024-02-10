package me.jellysquid.mods.lithium.mixin.collections.goals;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Supplier;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {

    @Mutable
    @Shadow
    @Final
    private Set<PrioritizedGoal> goals;

    /**
     * Replace the goal set with an optimized collection type which performs better for iteration.
     */
    @Inject(method = "<init>(Ljava/util/function/Supplier;)V", at = @At("RETURN"))
    private void reinit(Supplier<Profiler> supplier, CallbackInfo ci) {
        this.goals = new ObjectLinkedOpenHashSet<>(this.goals);
    }
}
