package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.utils.Color;

public abstract class DoubleTextHudModule extends HudModule {
    protected Color rightColor;
    protected boolean visible = true;

    private String left;
    private String right;

    private double leftWidth;

    public DoubleTextHudModule(HUD hud, String name, String description, String left) {
        super(hud, name, description);

        this.rightColor = hud.secondaryColor();
        this.left = left;
    }

    @Override
    public void update(HudRenderer renderer) {
        right = getRight();
        leftWidth = renderer.textWidth(left);

        box.setSize(leftWidth + renderer.textWidth(right), renderer.textHeight());
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!visible) return;

        int x = box.getX();
        int y = box.getY();

        renderer.text(left, x, y, hud.primaryColor());
        renderer.text(right, x + leftWidth, y, rightColor);
    }

    protected void setLeft(String left) {
        this.left = left;
        this.leftWidth = 0;
    }

    protected abstract String getRight();
}
