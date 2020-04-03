package minegame159.meteorclient.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

public class WItem extends WWidget {
    public ItemStack itemStack;

    public WItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(16, 16);
    }

    @Override
    public void onRenderPost(double delta) {
        DiffuseLighting.enable();
        RenderSystem.enableDepthTest();
        MinecraftClient.getInstance().getItemRenderer().renderGuiItem(itemStack, (int) boundingBox.getInnerX(), (int) boundingBox.getInnerY());
    }
}
