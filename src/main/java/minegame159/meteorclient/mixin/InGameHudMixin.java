package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow private int scaledWidth;

    @Shadow private int scaledHeight;

    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(float tickDelta, CallbackInfo info) {
        client.getProfiler().swap("meteor-client_render");

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        MeteorClient.eventBus.post(EventStore.render2DEvent(scaledWidth, scaledHeight, tickDelta));

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.lineWidth(1);
        GlStateManager.disableBlend();
        GlStateManager.disableBlend();
    }
}
