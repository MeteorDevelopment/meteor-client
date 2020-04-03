package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class WModule extends WWidget {
    private Module module;

    private double animationProgress1;
    private double animationMultiplier1;

    private double animationProgress2;
    private double animationMultiplier2;

    public WModule(Module module) {
        boundingBox.autoSize = true;
        boundingBox.fullWidth = true;
        boundingBox.setMargin(4);
        tooltip = module.description;

        this.module = module;
        if (module instanceof ToggleModule && ((ToggleModule) module).isActive()) {
            animationProgress1 = 1;
            animationMultiplier1 = 1;

            animationProgress2 = 1;
            animationMultiplier2 = 1;
        } else {
            animationProgress1 = 0;
            animationMultiplier1 = -1;

            animationProgress2 = 0;
            animationMultiplier2 = -1;
        }

        add(new WLabel(module.title));
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            if (button == 0) module.doAction(MinecraftClient.getInstance().world != null);
            else if (button == 1) module.openScreen();

            return true;
        }

        return false;
    }

    @Override
    public void onRender(double delta) {
        if (module instanceof ToggleModule) {
            if (((ToggleModule) module).isActive()) {
                animationMultiplier1 = 1;
                animationMultiplier2 = 1;
            }
            else {
                animationMultiplier1 = -1;
                animationMultiplier2 = -1;
            }
        }

        if (mouseOver) animationMultiplier1 = 1;
        else {
            if (module instanceof ToggleModule) {
                if (!((ToggleModule) module).isActive()) animationMultiplier1 = -1;
            } else animationMultiplier1 = -1;
        }

        animationProgress1 += delta / 10 * GUI.hoverAnimationSpeedMultiplier * animationMultiplier1;
        animationProgress1 = Utils.clamp(animationProgress1, 0, 1);

        animationProgress2 += delta / 10 * GUI.hoverAnimationSpeedMultiplier * animationMultiplier2;
        animationProgress2 = Utils.clamp(animationProgress2, 0, 1);

        if (animationProgress1 > 0  || animationProgress2 > 0) {
            RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth() * animationProgress1, boundingBox.getHeight(), GUI.backgroundModuleActive);
            RenderUtils.quad(boundingBox.x, boundingBox.y + boundingBox.getHeight() * (1 - animationProgress2), 1, boundingBox.getHeight() * animationProgress2, GUI.accent);
        }
    }
}
