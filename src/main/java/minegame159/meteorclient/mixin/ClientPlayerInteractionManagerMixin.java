package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.AttackEntityEvent;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.StartBreakingBlockEvent;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.Reach;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin implements IClientPlayerInteractionManager {
    @Shadow protected abstract void syncSelectedSlot();

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        AttackEntityEvent event = EventStore.attackEntityEvent(target);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "method_2902", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
        StartBreakingBlockEvent event = EventStore.startBreakingBlockEvent(blockPos, direction);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true)
    private void onGetReachDistance(CallbackInfoReturnable<Float> info) {
        if (ModuleManager.INSTANCE.isActive(Reach.class)) info.setReturnValue(ModuleManager.INSTANCE.get(Reach.class).getReach());
    }

    @Override
    public void syncSelectedSlot2() {
        syncSelectedSlot();
    }
}
