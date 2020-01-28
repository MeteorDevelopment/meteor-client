package minegame159.meteorclient.clickgui.widgets;

import net.minecraft.client.MinecraftClient;

public class WindowCenter extends Widget {
    public WindowCenter(double margin) {
        super(0, 0, margin);
    }

    @Override
    public void layout() {
        super.layout();
        calculateSize();

        x = (MinecraftClient.getInstance().getWindow().getScaledWidth() - width) / 2;
        y = (MinecraftClient.getInstance().getWindow().getScaledHeight() - height) / 2;

        super.layout();
    }

    @Override
    public void onWindowResized(double windowWidth, double windowHeight) {
        layout();
        super.onWindowResized(windowWidth, windowHeight);
    }
}
