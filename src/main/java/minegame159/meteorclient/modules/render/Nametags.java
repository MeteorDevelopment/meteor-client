package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.client.render.VertexFormats;
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

    public void render(double dist, float entityHeight, double x, double y, double z, float cameraYaw, float cameraPitch, String name, int health, int maxHealth) {
        float scale = 0.025f;
        if (dist > 10) scale *= dist / 10 * this.scale.get();

        float yOffset = entityHeight + 0.5F;
        int verticalOffset = "deadmau5".equals(name) ? -10 : 0;

        GlStateManager.pushMatrix();
        GlStateManager.translatef((float) x, (float) y + yOffset, (float) z);
        GlStateManager.normal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(-cameraYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotatef(cameraPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scalef(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture();
        GlStateManager.disableDepthTest();

        String healthText = health + "";
        double halfWidthName = Utils.getTextWidth(name) / 2.0;
        double halfWidthHealth = Utils.getTextWidth(healthText) / 2.0;
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

        int nameColor = FriendManager.INSTANCE.getColor(name).getPacked();

        double percHealth = (double) health / maxHealth;
        int healthColor;
        if (percHealth <= 0.333) healthColor = Color.fromRGBA(225, 45, 45, 255);
        else if (percHealth <= 0.666) healthColor = Color.fromRGBA(225, 105, 25, 255);
        else healthColor = Color.fromRGBA(45, 225, 45, 255);

        Utils.drawText(name, (float) (-halfWidth), (float) verticalOffset, nameColor);
        Utils.drawText(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
        Utils.drawText(name, (float) (-halfWidth), (float) verticalOffset, nameColor);
        Utils.drawText(healthText, (float) (-halfWidth + halfWidthName * 2 + 4), (float) verticalOffset, healthColor);

        GlStateManager.enableDepthTest();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
