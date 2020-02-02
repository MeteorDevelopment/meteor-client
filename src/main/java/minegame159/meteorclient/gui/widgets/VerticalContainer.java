package minegame159.meteorclient.gui.widgets;

public class VerticalContainer extends Widget {
    private double spacing;

    public VerticalContainer(double margin, double spacing) {
        super(0, 0, margin);
        this.spacing = spacing;
    }

    @Override
    public void layout() {
        double wY = y + margin;

        for (Widget widget : widgets) {
            widget.x = x + margin;
            widget.y = wY;

            widget.layout();

            wY += widget.heightMargin() + spacing;
        }

        calculateSize();
    }

    public void setSpacing(double spacing) {
        this.spacing = spacing;
        layout();
    }
}
