package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public ClientWorld world;

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        if (Utils.canUpdate()) {
            world.getProfiler().swap("meteor-client_update");
            MeteorClient.eventBus.post(EventStore.tickEvent());
        }
    }
}
