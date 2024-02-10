package me.jellysquid.mods.lithium.mixin.block.hopper;

import net.minecraft.command.EntityDataObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes hoppers not noticing that the item type of item entities changed after running the data command.
 */
@Mixin(EntityDataObject.class)
public class EntityDataObjectMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(
            method = "setNbt(Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setUuid(Ljava/util/UUID;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void updateEntityTrackerEngine(NbtCompound nbt, CallbackInfo ci) {
        Entity entity = this.entity;
        if (entity instanceof ItemEntity) {
            ((EntityAccessor) entity).getChangeListener().updateEntityPosition();
        }
    }
}
