package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Nametags;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow @Final protected EntityRenderDispatcher renderManager;

    @Shadow public abstract TextRenderer getFontRenderer();

    @Inject(method = "renderLabel(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, String text, double x, double y, double z, int maxDistance, CallbackInfo info) {
        if (!(entity instanceof PlayerEntity)) return;
        if (ModuleManager.INSTANCE.isActive(Nametags.class)) info.cancel();
        else return;

        double dist = Math.sqrt(entity.squaredDistanceTo(renderManager.camera.getPos()));
        float scale = 0.025f;
        if (dist > 10) scale *= dist / 10 * ModuleManager.INSTANCE.get(Nametags.class).getScale();

        float yOffset = entity.getHeight() + 0.5F;
        int verticalOffset = "deadmau5".equals(text) ? -10 : 0;

        GlStateManager.pushMatrix();
        GlStateManager.translatef((float) x, (float) y + yOffset, (float) z);
        GlStateManager.normal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(-renderManager.cameraYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(renderManager.cameraPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scalef(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture();
        GlStateManager.disableDepthTest();

        int health = (int) ((PlayerEntity) entity).getHealth();
        String healthText = health + "";
        double halfWidthName = getFontRenderer().getStringWidth(text) / 2.0;
        double halfWidthHealth = getFontRenderer().getStringWidth(healthText) / 2.0;
        double halfWidth = halfWidthName + 4 + halfWidthHealth - 2.5;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bb = tessellator.getBuffer();
        bb.begin(7, VertexFormats.POSITION_COLOR);
        double bx1 = -halfWidth - 1;
        double bx2 = halfWidth + 1;
        double by1 = verticalOffset - 1;
        double by2 = verticalOffset + 9;
        // Background
        bb.vertex(bx1, by1, 0.0).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(bx1, by2, 0.0).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(bx2, by2, 0.0).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(bx2, by1, 0.0).color(0f, 0f, 0f, 0.5f).next();
        // Left Edge
        bb.vertex(bx1 - 1, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx1 - 1, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx1, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx1, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        // Right Edge
        bb.vertex(bx2, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        // Top Edge
        bb.vertex(bx1 - 1, by1 - 1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx1 - 1, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by1 - 1, 0.0).color(0f, 0f, 0f, 1f).next();
        // Bottom Edge
        bb.vertex(bx1 - 1, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx1 - 1, by2 + 1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by2 + 1, 0.0).color(0f, 0f, 0f, 1f).next();
        bb.vertex(bx2 + 1, by2, 0.0).color(0f, 0f, 0f, 1f).next();
        tessellator.draw();

        GlStateManager.enableTexture();

        int nameColor = FriendManager.INSTANCE.contains((PlayerEntity) entity) ? DyeColor.CYAN.getSignColor() : -1;

        int healthColor;
        if (health <= 6) healthColor = Color.fromRGBA(225, 45, 45, 255);
        else if (health <= 12) healthColor = Color.fromRGBA(225, 105, 25, 255);
        else healthColor = Color.fromRGBA(45, 225, 45, 255);

        Utils.drawText(text, (float) (-halfWidth), (float) verticalOffset, nameColor);
        Utils.drawText(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
        Utils.drawText(text, (float) (-halfWidth), (float) verticalOffset, nameColor);
        Utils.drawText(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor);

        GlStateManager.enableDepthTest();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
