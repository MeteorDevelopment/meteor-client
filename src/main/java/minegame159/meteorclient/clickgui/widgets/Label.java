package minegame159.meteorclient.clickgui.widgets;

import minegame159.meteorclient.clickgui.WidgetColors;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.font.TextRenderer;

public class Label extends Widget {
    private String text;
    public boolean shadow;

    public Label(double margin, String text, boolean shadow) {
        super(Utils.getTextWidth(text) - 1, Utils.getTextHeight() - 2, margin);
        this.text = text;
        this.shadow = shadow;
    }

    public Label(double margin, String text) {
        this(margin, text, false);
    }

    public void setText(String text) {
        this.text = text;
        width = Utils.getTextWidth(text) - 1;
        height = Utils.getTextHeight() - 1;

        parentLayout();
    }

    @Override
    public void render(double mouseX, double mouseY) {
        super.render(mouseX, mouseY);
    }

    @Override
    public void renderText(double mouseX, double mouseY, TextRenderer font) {
        if (shadow) font.drawWithShadow(text, (float) (x + margin), (float) (y + margin), WidgetColors.textC);
        else font.draw(text, (float) (x + margin), (float) (y + margin), WidgetColors.textC);

        super.renderText(mouseX, mouseY, font);
    }
}
