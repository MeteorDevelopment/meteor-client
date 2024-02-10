package me.jellysquid.mods.lithium.mixin.ai.task.replace_streams;

import me.jellysquid.mods.lithium.common.ai.WeightedListIterable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.CompositeTask;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(CompositeTask.class)
public abstract class CompositeTaskMixin<E extends LivingEntity> implements Task<E> {
    @Shadow
    @Final
    private WeightedList<Task<? super E>> tasks;

    @Shadow
    @Final
    private Set<MemoryModuleType<?>> memoriesToForgetWhenStopped;

    @Shadow
    private MultiTickTask.Status status;

    /**
     * @reason Replace stream code with traditional iteration
     * @author JellySquid, IMS, 2No2Name
     */
    @Override
    @Overwrite
    public final void tick(ServerWorld world, E entity, long time) {
        boolean hasOneTaskRunning = false;
        for (Task<? super E> task : WeightedListIterable.cast(this.tasks)) {
            if (task.getStatus() == MultiTickTask.Status.RUNNING) {
                task.tick(world, entity, time);
                hasOneTaskRunning |= task.getStatus() == MultiTickTask.Status.RUNNING;
            }
        }

        if (!hasOneTaskRunning) {
            this.stop(world, entity, time);
        }
    }

    /**
     * @reason Replace stream code with traditional iteration
     * @author JellySquid
     */
    @Override
    @Overwrite
    public final void stop(ServerWorld world, E entity, long time) {
        this.status = MultiTickTask.Status.STOPPED;
        for (Task<? super E> task : WeightedListIterable.cast(this.tasks)) {
            if (task.getStatus() == MultiTickTask.Status.RUNNING) {
                task.stop(world, entity, time);
            }
        }

        Brain<?> brain = entity.getBrain();

        for (MemoryModuleType<?> module : this.memoriesToForgetWhenStopped) {
            brain.forget(module);
        }
    }
}