package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class WModule extends WWidget {
    private Module module;

    private double animationProgress;
    private double animationMultiplier;

    public WModule(Module module) {
        boundingBox.autoSize = true;
        tooltip = module.description;

        this.module = module;
        if (module.isActive()) {
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
        if (mouseOver && button == 0) {
            if (module.setting) module.openScreen();
            else module.toggle();
            return true;
        } else if (mouseOver && button == 1) {
            module.openScreen();
            return true;
        }

        return false;
    }

    @Override
    public void onRender(double delta) {
        if (mouseOver) animationMultiplier = 1;
        else animationMultiplier = -1;
        if (module.isActive()) animationMultiplier = 1;

        animationProgress += delta / 10 * GUI.hoverAnimationSpeedMultiplier * animationMultiplier;
        animationProgress = Utils.clamp(animationProgress, 0, 1);

        if (animationProgress > 0) {
            if (animationProgress == 1) RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), boundingBox.getHeight(), GUI.backgroundHighlighted);
            else {
                switch (GUI.hoverAnimation) {
                    case FromLeft:
                        RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth() * animationProgress, boundingBox.getHeight(), GUI.backgroundHighlighted);
                        break;
                    case FromCenter:
                        RenderUtils.quad(boundingBox.x + boundingBox.getWidth() / 2 - boundingBox.getWidth() / 2 * animationProgress, boundingBox.y, boundingBox.getWidth() * animationProgress, boundingBox.getHeight(), GUI.backgroundHighlighted);
                        break;
                    case FromRight:
                        RenderUtils.quad(boundingBox.x + boundingBox.getWidth(), boundingBox.y, -boundingBox.getWidth() * animationProgress, boundingBox.getHeight(), GUI.backgroundHighlighted);
                        break;
                }
            }
        }

        RenderUtils.line(boundingBox.x, boundingBox.y, boundingBox.x + boundingBox.getWidth(), boundingBox.y, GUI.outline);
        RenderUtils.line(boundingBox.x, boundingBox.y + boundingBox.getHeight(), boundingBox.x + boundingBox.getWidth(), boundingBox.y + boundingBox.getHeight(), GUI.outline);
        RenderUtils.line(boundingBox.x, boundingBox.y, boundingBox.x, boundingBox.y + boundingBox.getHeight(), GUI.outline);
        RenderUtils.line(boundingBox.x + boundingBox.getWidth(), boundingBox.y, boundingBox.x + boundingBox.getWidth(), boundingBox.y + boundingBox.getHeight(), GUI.outline);
    }
}
