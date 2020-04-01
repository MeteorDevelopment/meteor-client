package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;

public class WModule extends WWidget {
    private Module module;

    private double animationProgress;
    private double animationMultiplier;

    public WModule(Module module) {
        boundingBox.autoSize = true;
        boundingBox.fullWidth = true;
        boundingBox.setMargin(4);
        tooltip = module.description;

        this.module = module;
        if (module instanceof ToggleModule && ((ToggleModule) module).isActive()) {
            animationProgress = 1;
            animationMultiplier = 1;
        } else {
            animationProgress = 0;
            animationMultiplier = -1;
        }

        add(new WLabel(module.title));
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            if (button == 0) module.doAction(false);
            else if (button == 1) module.openScreen();

            return true;
        }

        return false;
    }

    @Override
    public void onRender(double delta) {
        if (module instanceof ToggleModule) {
            if (((ToggleModule) module).isActive()) animationMultiplier = 1;
            else animationMultiplier = -1;
        }

        animationProgress += delta / 10 * GUI.hoverAnimationSpeedMultiplier * animationMultiplier;
        animationProgress = Utils.clamp(animationProgress, 0, 1);

        if (animationProgress > 0) {
            RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth() * animationProgress, boundingBox.getHeight(), GUI.backgroundModuleActive);
            RenderUtils.quad(boundingBox.x, boundingBox.y + boundingBox.getHeight() * (1 - animationProgress), 1, boundingBox.getHeight() * animationProgress, GUI.accent);
        }
    }
}
