package me.jellysquid.mods.lithium.mixin.entity.inactive_navigations;

import me.jellysquid.mods.lithium.common.entity.NavigatingEntity;
import me.jellysquid.mods.lithium.common.world.ServerWorldExtended;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityNavigation.class)
public abstract class EntityNavigationMixin {

    @Shadow
    @Final
    protected World world;

    @Shadow
    protected Path currentPath;

    @Shadow
    @Final
    protected MobEntity entity;

    @Inject(
            method = "recalculatePath()V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/entity/ai/pathing/EntityNavigation;findPathTo(Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/entity/ai/pathing/Path;",
                    shift = At.Shift.AFTER
            )
    )
    private void updateListeningState(CallbackInfo ci) {
        if (((NavigatingEntity) this.entity).isRegisteredToWorld()) {
            if (this.currentPath == null) {
                ((ServerWorldExtended) this.world).setNavigationInactive(this.entity);
            } else {
                ((ServerWorldExtended) this.world).setNavigationActive(this.entity);
            }
        }
    }

    @Inject(method = "startMovingAlong(Lnet/minecraft/entity/ai/pathing/Path;D)Z", at = @At(value = "RETURN"))
    private void updateListeningState2(Path path, double speed, CallbackInfoReturnable<Boolean> cir) {
        if (((NavigatingEntity) this.entity).isRegisteredToWorld()) {
            if (this.currentPath == null) {
                ((ServerWorldExtended) this.world).setNavigationInactive(this.entity);
            } else {
                ((ServerWorldExtended) this.world).setNavigationActive(this.entity);
            }
        }
    }

    @Inject(method = "stop()V", at = @At(value = "RETURN"))
    private void stopListening(CallbackInfo ci) {
        if (((NavigatingEntity) this.entity).isRegisteredToWorld()) {
            ((ServerWorldExtended) this.world).setNavigationInactive(this.entity);
        }
    }
}
