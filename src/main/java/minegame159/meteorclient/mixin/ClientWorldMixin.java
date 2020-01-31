package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void onAddEntity(int id, Entity entity, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.entityAddedEvent(entity));
    }

    @Inject(method = "finishRemovingEntity", at = @At("TAIL"))
    private void onFinishRemovingEntity(Entity entity, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.entityRemovedEvent(entity));
    }
}
