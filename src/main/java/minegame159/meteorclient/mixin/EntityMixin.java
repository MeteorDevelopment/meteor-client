package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.NoPush;
import minegame159.meteorclient.modules.movement.SafeWalk;
import minegame159.meteorclient.modules.movement.Scaffold;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void onSetVelocityClient(double x, double y, double z, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        if (ModuleManager.INSTANCE.isActive(NoPush.class)) {
            info.cancel();
        }
    }

    @Inject(method = "addVelocity", at = @At("HEAD"), cancellable = true)
    private void onAddVelocity(double deltaX, double deltaY, double deltaZ, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        if (ModuleManager.INSTANCE.isActive(NoPush.class)) {
            info.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        MeteorClient.EVENT_BUS.post(EventStore.playerMoveEvent(type, movement));
    }

    @Redirect(method = "adjustMovementForSneaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    private boolean isSafeWalkSneaking(Entity entity) {
        Scaffold scaffold = ModuleManager.INSTANCE.get(Scaffold.class);
        return entity.isSneaking() || ModuleManager.INSTANCE.isActive(SafeWalk.class) || (scaffold.isActive() && scaffold.hasSafeWalk());
    }
}
