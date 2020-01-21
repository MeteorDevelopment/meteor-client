package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow private int scaledWidth;

    @Shadow private int scaledHeight;

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(float tickDelta, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.render2DEvent(scaledWidth, scaledHeight, tickDelta));
    }
}
