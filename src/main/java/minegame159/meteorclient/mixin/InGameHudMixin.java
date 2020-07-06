package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.HUD;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.MathHelper;
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

    @Shadow public abstract void clear();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrixStack, float tickDelta, CallbackInfo info) {
        client.getProfiler().swap("meteor-client_render");

        RenderSystem.pushMatrix();
        invertBobViewWhenHurt(tickDelta);
        invertBobView(tickDelta);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        MeteorClient.EVENT_BUS.post(EventStore.render2DEvent(scaledWidth, scaledHeight, tickDelta));

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1);
        RenderSystem.disableBlend();
        RenderSystem.disableBlend();

        RenderSystem.popMatrix();
    }

    private void invertBobViewWhenHurt(float f) {
        if (this.client.getCameraEntity() instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)this.client.getCameraEntity();
            float g = (float)livingEntity.hurtTime - f;
            float i;
            Matrix4f m;
            if (livingEntity.getHealth() <= 0.0F) {
                i = Math.min((float)livingEntity.deathTime + f, 20.0F);
                m = new Matrix4f(Vector3f.POSITIVE_Z.getDegreesQuaternion(40.0F - 8000.0F / (i + 200.0F)));
                m.invert();
                RenderSystem.multMatrix(m);
            }

            if (g < 0.0F) {
                return;
            }

            g /= (float)livingEntity.maxHurtTime;
            g = MathHelper.sin(g * g * g * g * 3.1415927F);
            i = livingEntity.knockbackVelocity;
            m = new Matrix4f(Vector3f.POSITIVE_Y.getDegreesQuaternion(-i));
            m.invert();
            RenderSystem.multMatrix(m);
            m = new Matrix4f(Vector3f.POSITIVE_Z.getDegreesQuaternion(-g * 14.0F));
            m.invert();
            RenderSystem.multMatrix(m);
            m = new Matrix4f(Vector3f.POSITIVE_Y.getDegreesQuaternion(i));
            m.invert();
            RenderSystem.multMatrix(m);
        }

    }

    private void invertBobView(float f) {
        if (client.options.bobView && client.getCameraEntity() instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)this.client.getCameraEntity();
            float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
            float h = -(playerEntity.horizontalSpeed + g * f);
            float i = MathHelper.lerp(f, playerEntity.field_7505, playerEntity.field_7483);
            RenderSystem.translated(-(MathHelper.sin(h * 3.1415927F) * i * 0.5F), -(-Math.abs(MathHelper.cos(h * 3.1415927F) * i)), 0.0D);
            Matrix4f m = new Matrix4f(Vector3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.sin(h * 3.1415927F) * i * 3.0F));
            m.invert();
            RenderSystem.multMatrix(m);
            m = new Matrix4f(Vector3f.POSITIVE_X.getDegreesQuaternion(Math.abs(MathHelper.cos(h * 3.1415927F - 0.2F) * i) * 5.0F));
            m.invert();
            RenderSystem.multMatrix(m);
        }
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
    private void onRenderScoreboardSidebar(MatrixStack matrixStack, ScoreboardObjective scoreboardObjective, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noScoreboard()) info.cancel();
    }
}
