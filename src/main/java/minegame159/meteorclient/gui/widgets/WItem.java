package minegame159.meteorclient.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {
    public ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        width = 32;
        height = 32;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.post(() -> {
            GlStateManager.enableTexture();
            DiffuseLighting.enable();
            GlStateManager.enableDepthTest();

            Window window = MinecraftClient.getInstance().getWindow();
            double s = window.getScaleFactor();

            GlStateManager.pushMatrix();
            GlStateManager.translated(-x * (s - 1), -y * (s - 1), 0);
            GlStateManager.scaled(s, s, 1);
            MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(itemStack, (int) x, (int) y);
            GlStateManager.popMatrix();
        });
    }
}
