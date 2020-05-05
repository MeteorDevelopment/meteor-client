package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.accountsfriends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.DyeColor;

public class Nametags extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public Nametags() {
        super(Category.Render, "nametags", "Displays nametags above players.");
    }

    public void render(MatrixStack matrixStack, double dist, float entityHeight, double x, double y, double z, float cameraYaw, float cameraPitch, String name, int health, int maxHealth) {
        float scale = 0.025f;
        if (dist > 10) scale *= dist / 10 * this.scale.get();

        float yOffset = entityHeight + 0.5F;
        int verticalOffset = "deadmau5".equals(name) ? -10 : 0;

        matrixStack.push();
        matrixStack.translate(0, yOffset, 0);
        matrixStack.multiply(mc.getEntityRenderManager().getRotation());
        matrixStack.scale(-scale, -scale, scale);
        Matrix4f matrix4f = matrixStack.peek().getModel();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();

        String healthText = health + "";
        double halfWidthName = mc.textRenderer.getStringWidth(name) / 2.0;
        double halfWidthHealth = mc.textRenderer.getStringWidth(healthText) / 2.0;
        double halfWidth = halfWidthName + 4 + halfWidthHealth - 2.5;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bb = tessellator.getBuffer();
        bb.begin(7, VertexFormats.POSITION_COLOR);
        double bx1 = -halfWidth - 1;
        double bx2 = halfWidth + 1;
        double by1 = verticalOffset - 1;
        double by2 = verticalOffset + 9;
        // Background
        bb.vertex(matrix4f, (float) bx1, (float) by1, 0.0f).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(matrix4f, (float) bx1, (float) by2, 0.0f).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(matrix4f, (float) bx2, (float) by2, 0.0f).color(0f, 0f, 0f, 0.5f).next();
        bb.vertex(matrix4f, (float) bx2, (float) by1, 0.0f).color(0f, 0f, 0f, 0.5f).next();
        // Left Edge
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx1, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx1, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        // Right Edge
        bb.vertex(matrix4f, (float) bx2, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        // Top Edge
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by1 - 1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by1 - 1, 0.0f).color(0f, 0f, 0f, 1f).next();
        // Bottom Edge
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx1 - 1, (float) by2 + 1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by2 + 1, 0.0f).color(0f, 0f, 0f, 1f).next();
        bb.vertex(matrix4f, (float) bx2 + 1, (float) by2, 0.0f).color(0f, 0f, 0f, 1f).next();
        tessellator.draw();

        RenderSystem.enableTexture();

        int nameColor = FriendManager.INSTANCE.getColor(name).getPacked();

        double percHealth = (double) health / maxHealth;
        int healthColor;
        if (percHealth <= 0.333) healthColor = Color.fromRGBA(225, 45, 45, 255);
        else if (percHealth <= 0.666) healthColor = Color.fromRGBA(225, 105, 25, 255);
        else healthColor = Color.fromRGBA(45, 225, 45, 255);

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        mc.textRenderer.draw(name, (float) (-halfWidth), (float) verticalOffset, nameColor, false, matrix4f, immediate, false, 0, 15728880);
        mc.textRenderer.draw(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor, false, matrix4f, immediate, false, 0, 15728880);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        mc.textRenderer.draw(name, (float) (-halfWidth), (float) verticalOffset, nameColor, false, matrix4f, immediate, false, 0, 15728880);
        mc.textRenderer.draw(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor, false, matrix4f, immediate, false, 0, 15728880);
        immediate.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.enableLighting();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.pop();
    }
}
