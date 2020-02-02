package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.ModuleScreen;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.setting.GUI;
import net.minecraft.client.MinecraftClient;

public class ModuleWidget extends Widget {
    private Module module;

    private long current, last;

    private boolean mouseOver, lastMouseOver;
    private double hoverProgress;
    private boolean hoverProgressIncreasing;

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
    public void onMouseMoved(double mouseX, double mouseY) {
        mouseOver = isMouseOver(mouseX, mouseY);
        if (!lastMouseOver && mouseOver) hoverProgressIncreasing = true;
        else if (lastMouseOver && !mouseOver) hoverProgressIncreasing = false;
        lastMouseOver = mouseOver;

        super.onMouseMoved(mouseX, mouseY);
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
        current = System.currentTimeMillis();
        if (last == 0) last = current;
        double delta = (current - last) / 1000.0;
        last = current;

        if (hoverProgressIncreasing || module.isActive()) {
            hoverProgress += delta * GUI.hoverAnimationSpeedMultiplier;
            if (hoverProgress > 1) hoverProgress = 1;
        } else {
            hoverProgress -= delta * GUI.hoverAnimationSpeedMultiplier;
            if (hoverProgress < 0) hoverProgress = 0;
        }

        quad(x + margin, y, x + parent.width - margin, y + heightMargin(), GUI.background);

        if (mouseOver || (!hoverProgressIncreasing && hoverProgress > 0)) {
            if (GUI.hoverAnimation == GUI.HoverAnimation.FromLeft) {
                quad(x + margin, y, x + margin + hoverProgress * parent.width - hoverProgress * (margin * 2), y + heightMargin(), GUI.backgroundHighlighted);
            } else if (GUI.hoverAnimation == GUI.HoverAnimation.FromRight) {
                quad(x + parent.width - margin, y, x + parent.width - margin - hoverProgress * parent.width + hoverProgress * (margin * 2), y + heightMargin(), GUI.backgroundHighlighted);
            } else if (GUI.hoverAnimation == GUI.HoverAnimation.FromCenter) {
                double x = this.x + parent.width / 2;

                quad(x, y, x + (hoverProgress / 2) * parent.width - (hoverProgress / 2) * (margin * 2), y + heightMargin(), GUI.backgroundHighlighted);
                quad(x - (hoverProgress / 2) * parent.width + (hoverProgress / 2) * (margin * 2), y, x, y + heightMargin(), GUI.backgroundHighlighted);
            }
        }

        super.render(mouseX, mouseY);
    }
}
