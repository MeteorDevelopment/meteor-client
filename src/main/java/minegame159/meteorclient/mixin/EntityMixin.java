package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.NoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public float yaw;
    @Shadow protected Vec3d movementMultiplier;
    private NoPush noPush;

    private NoPush getNoPush() {
        if (noPush == null) noPush = ModuleManager.INSTANCE.get(NoPush.class);
        return noPush;
    }

    @Inject(method = "setVelocityClient", at = @At("HEAD"), cancellable = true)
    private void onSetVelocityClient(double x, double y, double z, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        if (getNoPush().isActive()) {
            info.cancel();
        }
    }

    @Inject(method = "addVelocity", at = @At("HEAD"), cancellable = true)
    private void onAddVelocity(double deltaX, double deltaY, double deltaZ, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        if (getNoPush().isActive()) {
            info.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        MeteorClient.eventBus.post(EventStore.playerMoveEvent(type, movement));
    }
}
