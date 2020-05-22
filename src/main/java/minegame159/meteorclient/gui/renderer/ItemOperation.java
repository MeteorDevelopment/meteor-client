package minegame159.meteorclient.gui.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

public class ItemOperation extends Operation {
    private double x, y;
    private ItemStack itemStack;

    public ItemOperation set(double x, double y, ItemStack itemStack) {
        this.x = x;
        this.y = y;
        this.itemStack = itemStack;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        GlStateManager.enableTexture();
        DiffuseLighting.enable();
        GlStateManager.enableDepthTest();
        MinecraftClient.getInstance().getItemRenderer().renderGuiItem(itemStack, (int) x, (int) y);
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.itemOperationPool.free(this);
    }
}
