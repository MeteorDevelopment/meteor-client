package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IPlayerEntity;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.SafeWalk;
import minegame159.meteorclient.modules.movement.Scaffold;
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
        Scaffold scaffold = ModuleManager.INSTANCE.get(Scaffold.class);
        if (ModuleManager.INSTANCE.isActive(SafeWalk.class) || (scaffold.isActive() && scaffold.hasSafeWalk())) {
            info.setReturnValue(true);
        }
    }

    @Override
    public void setInventory(PlayerInventory inventory) {
        this.inventory = inventory;
    }
}
