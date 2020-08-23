package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.events.AttackEntityEvent;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.StartBreakingBlockEvent;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.combat.AntiFriendHit;
import minegame159.meteorclient.modules.misc.Nuker;
import minegame159.meteorclient.modules.player.Reach;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
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

    @Shadow private int field_3716;

    @Shadow private float currentBreakingProgress;

    @Shadow private BlockPos currentBreakingPos;

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        AttackEntityEvent event = EventStore.attackEntityEvent(target);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
        if(target instanceof PlayerEntity &&  ModuleManager.INSTANCE.get(AntiFriendHit.class).isActive() && !FriendManager.INSTANCE.attack((PlayerEntity) target)) info.cancel();
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

    @Redirect(method = "method_2902", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;field_3716:I", opcode = Opcodes.PUTFIELD))
    private void onMethod_2902SetField_3716Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (ModuleManager.INSTANCE.isActive(Nuker.class)) value = 0;
        field_3716 = value;
    }

    @Redirect(method = "attackBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;field_3716:I", opcode = Opcodes.PUTFIELD))
    private void onAttackBlockSetField_3719Proxy(ClientPlayerInteractionManager interactionManager, int value) {
        if (ModuleManager.INSTANCE.isActive(Nuker.class)) value = 0;
        field_3716 = value;
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> info) {
        MeteorClient.EVENT_BUS.post(EventStore.breakBlockEvent(blockPos));
    }

    @Override
    public void syncSelectedSlot2() {
        syncSelectedSlot();
    }

    @Override
    public double getBreakingProgress() {
        return currentBreakingProgress;
    }

    @Override
    public BlockPos getCurrentBreakingBlockPos() {
        return currentBreakingPos;
    }
}
