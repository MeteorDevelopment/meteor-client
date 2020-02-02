package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.ModuleScreen;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.MinecraftClient;

public class ModuleWidget extends Widget {
    private Module module;

    public ModuleWidget(Module module) {
        super(0, 0, 5);
        this.module = module;

        addWidget(new Label(0, module.title));

        tooltip = module.description;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x + margin && mouseX <= x + parent.width - margin && mouseY >= y && mouseY <= y + heightMargin();
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (module.setting) {
            if (isMouseOver(mouseX, mouseY) && (button == 0 || button == 1)) {
                MinecraftClient.getInstance().openScreen(new ModuleScreen(MinecraftClient.getInstance().currentScreen, module));
                return true;
            }
        } else {
            if (isMouseOver(mouseX, mouseY) && button == 0) {
                module.toggle();
                return true;
            } else if (isMouseOver(mouseX, mouseY) && button == 1) {
                MinecraftClient.getInstance().openScreen(new ModuleScreen(MinecraftClient.getInstance().currentScreen, module));
                return true;
            }
        }

        return super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void layout() {
        for (Widget widget : widgets) {
            widget.x = x + margin * 2;
            widget.y = y + margin;

            widget.layout();
        }

        calculateSize();
        width += margin;
    }

    @Override
    public void render(double mouseX, double mouseY) {
        Color backgroundColor = GUI.background;
        if (isMouseOver(mouseX, mouseY) || module.isActive()) backgroundColor = GUI.backgroundHighlighted;

        quad(x + margin, y, x + parent.width - margin, y + heightMargin(), backgroundColor);

        super.render(mouseX, mouseY);
    }
}
