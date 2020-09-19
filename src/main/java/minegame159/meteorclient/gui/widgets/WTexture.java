package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import net.minecraft.client.texture.AbstractTexture;

public class WTexture extends WWidget {
    private final double width, height;
    private final double rotation;
    private final AbstractTexture texture;

    public WTexture(double width, double height, double rotation, AbstractTexture texture) {
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.texture = texture;
    }

    @Override
    protected void onCalculateSize() {
        super.width = width;
        super.height = height;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderTexture(x, y, width, height, rotation, texture);
    }
}
