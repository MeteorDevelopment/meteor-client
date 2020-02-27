package minegame159.meteorclient.gui;

public class Alignment {
    public X x = X.Left;
    public Y y = Y.Top;

    public void set(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public enum X {
        Left, Center, Right
    }

    public enum Y {
        Top, Center, Bottom
    }
}
