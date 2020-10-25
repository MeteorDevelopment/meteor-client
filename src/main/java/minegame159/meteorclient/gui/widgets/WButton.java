package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.Color;

public class WButton extends WPressable {
    public enum ButtonRegion {
        Edit(Region.EDIT, GuiConfig.INSTANCE.edit, GuiConfig.INSTANCE.editHovered, GuiConfig.INSTANCE.editPressed),
        Reset(Region.RESET, GuiConfig.INSTANCE.reset, GuiConfig.INSTANCE.resetHovered, GuiConfig.INSTANCE.resetPressed);

        public final Region region;
        public final Color color, hovered, pressed;

        ButtonRegion(Region region, Color color, Color hovered, Color pressed) {
            this.region = region;
            this.color = color;
            this.hovered = hovered;
            this.pressed = pressed;
        }
    }

    private String text;
    private double textWidth;

    private final ButtonRegion region;

    public WButton(String text, ButtonRegion region) {
        this.text = text;
        this.textWidth = -1;

        this.region = region;
    }

    public WButton(String text) {
        this(text, null);
    }

    public WButton(ButtonRegion region) {
        this(null, region);
    }

    public void setText(String text) {
        this.text = text;
        this.textWidth = -1;

        invalidate();
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        if (textWidth == -1 && text != null) textWidth = renderer.textWidth(text);

        width = 6 + (text == null ? renderer.textHeight() : textWidth) + 6;
        height = 6 + renderer.textHeight() + 6;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.background(this, super.pressed);

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + 6, false, GuiConfig.INSTANCE.text);
        } else {
            Color color;
            if (pressed) color = region.pressed;
            else if (mouseOver) color = region.hovered;
            else color = region.color;

            renderer.quad(region.region, x + 6, y + 6, renderer.textHeight(), renderer.textHeight(), color);
        }
    }
}
