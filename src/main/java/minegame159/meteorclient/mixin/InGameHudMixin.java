package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.HUD;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
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

        MeteorClient.EVENT_BUS.post(EventStore.render2DEvent(scaledWidth, scaledHeight, tickDelta));

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.lineWidth(1);
        GlStateManager.disableBlend();
        GlStateManager.disableBlend();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(HUD.class) && ModuleManager.INSTANCE.get(HUD.class).potionTimers.get()) info.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(float f, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noPortalOverlay()) info.cancel();
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPumpkinOverlay(CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noPumpkinOverlay()) info.cancel();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderVignetteOverlay(Entity entity, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noVignette()) info.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardSidebar(ScoreboardObjective scoreboardObjective, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noScoreboard()) info.cancel();
    }
}
