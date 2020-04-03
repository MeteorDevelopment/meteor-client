package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IPlayerEntity;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.SafeWalk;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerEntity {
    @Shadow public PlayerInventory inventory;

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    protected void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        if (ModuleManager.INSTANCE.isActive(SafeWalk.class)) {
            info.setReturnValue(((PlayerEntity) (Object) this).isSneaking() || ModuleManager.INSTANCE.isActive(SafeWalk.class));
        }
    }

    @Override
    public void setInventory(PlayerInventory inventory) {
        this.inventory = inventory;
    }
}
