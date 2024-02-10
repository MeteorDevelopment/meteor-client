package me.jellysquid.mods.lithium.mixin.ai.task.launch;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.lithium.common.util.collections.MaskedList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.function.Supplier;

@Mixin(Brain.class)
public class BrainMixin<E extends LivingEntity> {

    @Shadow
    @Final
    private Map<Integer, Map<Activity, Set<Task<? super E>>>> tasks;

    @Shadow
    @Final
    private Set<Activity> possibleActivities;

    private ArrayList<Task<? super E>> possibleTasks;
    private MaskedList<Task<? super E>> runningTasks;

    private void onTasksChanged() {
        this.runningTasks = null;
        this.onPossibleActivitiesChanged();
    }

    private void onPossibleActivitiesChanged() {
        this.possibleTasks = null;
    }

    private void initPossibleTasks() {
        this.possibleTasks = new ArrayList<>();
        for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
            for (Map.Entry<Activity, Set<Task<? super E>>> entry : map.entrySet()) {
                Activity activity = entry.getKey();
                if (!this.possibleActivities.contains(activity)) {
                    continue;
                }
                Set<Task<? super E>> set = entry.getValue();
                for (Task<? super E> task : set) {
                    //noinspection UseBulkOperation
                    this.possibleTasks.add(task);
                }
            }
        }
    }

    private ArrayList<Task<? super E>> getPossibleTasks() {
        if (this.possibleTasks == null) {
            this.initPossibleTasks();
        }
        return this.possibleTasks;
    }

    private MaskedList<Task<? super E>> getCurrentlyRunningTasks() {
        if (this.runningTasks == null) {
            this.initCurrentlyRunningTasks();
        }
        return this.runningTasks;
    }

    private void initCurrentlyRunningTasks() {
        MaskedList<Task<? super E>> list = new MaskedList<>(new ObjectArrayList<>(), false);

        for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
            for (Set<Task<? super E>> set : map.values()) {
                for (Task<? super E> task : set) {
                    list.addOrSet(task, task.getStatus() == MultiTickTask.Status.RUNNING);
                }
            }
        }
        this.runningTasks = list;
    }

    /**
     * @author 2No2Name
     * @reason use optimized cached collection
     */
    @Overwrite
    private void startTasks(ServerWorld world, E entity) {
        long startTime = world.getTime();
        for (Task<? super E> task : this.getPossibleTasks()) {
            if (task.getStatus() == MultiTickTask.Status.STOPPED) {
                task.tryStarting(world, entity, startTime);
            }
        }
    }

    /**
     * @author 2No2Name
     * @reason use optimized cached collection
     */
    @Overwrite
    @Deprecated
    @Debug
    public List<Task<? super E>> getRunningTasks() {
        return this.getCurrentlyRunningTasks();
    }


    @Inject(
            method = "<init>(Ljava/util/Collection;Ljava/util/Collection;Lcom/google/common/collect/ImmutableList;Ljava/util/function/Supplier;)V",
            at = @At("RETURN")
    )
    private void reinitializeBrainCollections(Collection<?> memories, Collection<?> sensors, ImmutableList<?> memoryEntries, Supplier<?> codecSupplier, CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "setTaskList(Lnet/minecraft/entity/ai/brain/Activity;Lcom/google/common/collect/ImmutableList;Ljava/util/Set;Ljava/util/Set;)V",
            at = @At("RETURN")
    )
    private void reinitializeTasksSorted(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Task<?>>> indexedTasks, Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories, Set<MemoryModuleType<?>> forgettingMemories, CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "clear()V",
            at = @At("RETURN")
    )
    private void reinitializeTasksSorted(CallbackInfo ci) {
        this.onTasksChanged();
    }

    @Inject(
            method = "resetPossibleActivities(Lnet/minecraft/entity/ai/brain/Activity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void onPossibleActivitiesChanged(Activity except, CallbackInfo ci) {
        this.onPossibleActivitiesChanged();
    }


    @Inject(
            method = "stopAllTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/task/Task;stop(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;J)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void removeStoppedTask(ServerWorld world, E entity, CallbackInfo ci, long l, Iterator<?> it, Task<? super E> task) {
        if (this.runningTasks != null) {
            this.runningTasks.setVisible(task, false);
        }
    }

    @Inject(
            method = "updateTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/task/Task;tick(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;J)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void removeTaskIfStopped(ServerWorld world, E entity, CallbackInfo ci, long l, Iterator<?> it, Task<? super E> task) {
        if (this.runningTasks != null && task.getStatus() != MultiTickTask.Status.RUNNING) {
            this.runningTasks.setVisible(task, false);
        }
    }

    @ModifyVariable(
            method = "startTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/task/Task;tryStarting(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;J)Z",
                    shift = At.Shift.AFTER
            )
    )
    private Task<? super E> addStartedTasks(Task<? super E> task) {
        if (this.runningTasks != null && task.getStatus() == MultiTickTask.Status.RUNNING) {
            this.runningTasks.setVisible(task, true);
        }
        return task;
    }
}