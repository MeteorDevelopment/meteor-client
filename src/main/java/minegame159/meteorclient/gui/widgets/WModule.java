package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class WModule extends WWidget {
    private Module module;
    private double titleWidth;

    private double animationProgress1;
    private double animationMultiplier1;

    private double animationProgress2;
    private double animationMultiplier2;

    public WModule(Module module) {
        this.module = module;
        this.titleWidth = Utils.getTextWidth(module.title);
        this.tooltip = module.description;

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
    }

    @Override
    protected void onCalculateSize() {
        width = 4 + titleWidth + 4;
        height = 4 + Utils.getTextHeight() + 4;
    }

    @Override
    protected boolean onMouseClicked(int button) {
        if (mouseOver) {
            if (button == 0) module.doAction(MinecraftClient.getInstance().world != null);
            else if (button == 1) module.openScreen();

            return true;
        }

        return false;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
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
            renderer.renderQuad(x, y, width * animationProgress1, height, GuiConfig.INSTANCE.moduleBackground);
            renderer.renderQuad(x, y + height * (1 - animationProgress2), 1, height * animationProgress2, GuiConfig.INSTANCE.accent);
        }

        renderer.renderText(module.title, x + width / 2 - titleWidth / 2, y + 4.5, GuiConfig.INSTANCE.text, false);
    }
}
