package minegame159.meteorclient.clickgui.widgets;

public class Container extends Widget {
    private boolean centerX;
    private boolean right;

    private double fakeWidth;

    public Container(double margin, boolean centerX, boolean right) {
        super(0, 0, margin);
        this.centerX = centerX;
        this.right = right;

        if (centerX && right) this.right = false;
    }

    public Container(double margin) {
        this(margin, false, false);
    }

    @Override
    public void layout() {
        super.layout();
        calculateSize();

        if ((centerX || right) && parent != null) fakeWidth = parent.width - margin * 2;

        if (centerX) {
            for (Widget widget : widgets) {
                widget.x = x + margin + (fakeWidth - widget.widthMargin()) / 2;
                widget.layout();
            }
        } else if (right) {
            for (Widget widget : widgets) {
                widget.x = x + margin + fakeWidth - widget.widthMargin();
                widget.layout();
            }
        }
    }

    public void setCenterX(boolean centerX) {
        this.centerX = centerX;
        if (centerX) right = false;
        else right = false;
        layout();
    }

    public void setRight(boolean right) {
        this.right = right;
        if (right) centerX = false;
        else centerX = false;
        layout();
    }
}
