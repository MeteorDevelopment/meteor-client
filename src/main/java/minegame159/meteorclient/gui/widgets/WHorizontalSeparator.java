package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;

public class WHorizontalSeparator extends WWidget {
    private String text;
    private double textWidth;

    public WHorizontalSeparator(String text) {
        this.text = text;
        this.textWidth = text != null ? MeteorClient.FONT.getStringWidth(text) : 0;
    }

    public WHorizontalSeparator() {
        this(null);
    }

    @Override
    protected void onCalculateSize() {
        width = 0;
        height = text != null ? MeteorClient.FONT.getHeight() : 1;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double textStart = width / 2.0 - textWidth / 2.0 - 2;
        double textEnd = 2 + textStart + textWidth + 2;

        double offsetY = height / 2.0;

        if (text != null) {
            renderer.renderQuad(x, y+ offsetY, textStart, 1, GuiConfig.INSTANCE.separator);
            renderer.renderText(text, x + textStart + 2, y, GuiConfig.INSTANCE.separator, false);
            renderer.renderQuad(x + textEnd, y + offsetY, width - textEnd, 1, GuiConfig.INSTANCE.separator);
        } else {
            renderer.renderQuad(x, y, width, height, GuiConfig.INSTANCE.separator);
        }
    }
}
