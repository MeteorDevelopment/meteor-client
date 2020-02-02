package minegame159.meteorclient.gui.widgets;

public class HorizontalContainer extends Widget {
    private double spacing;
    private boolean centerWidgetsVertically;

    public HorizontalContainer(double margin, double spacing, boolean centerWidgetsVertically) {
        super(0, 0, margin);
        this.spacing = spacing;
        this.centerWidgetsVertically = centerWidgetsVertically;
    }

    public HorizontalContainer(double margin, double spacing) {
        this(margin, spacing, true);
    }

    @Override
    public void layout() {
        double wX = x + margin;

        for (Widget widget : widgets) {
            widget.x = wX;
            widget.y = y + margin;

            widget.layout();

            wX += widget.widthMargin() + spacing;
        }

        calculateSize();

        if (centerWidgetsVertically) {
            for (Widget widget : widgets) {
                widget.y = y + margin + (height - widget.heightMargin()) / 2;
                widget.layout();
            }
        }
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
        layout();
    }

    public void setCenterWidgetsVertically(boolean centerWidgetsVertically) {
        this.centerWidgetsVertically = centerWidgetsVertically;
        layout();
    }
}
