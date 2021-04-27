package minegame159.meteorclient.utils.render.color;

public class RainbowColor extends Color {

    private double speed;
    private static final float[] hsb = new float[3];

    public RainbowColor() {
        super();
    }

    public double getSpeed() {
        return speed;
    }

    public RainbowColor setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public RainbowColor getNext() {
        return getNext(1);
    }

    public RainbowColor getNext(double delta) {
        if (speed > 0) {
            java.awt.Color.RGBtoHSB(r, g, b, hsb);
            int c = java.awt.Color.HSBtoRGB(hsb[0] + (float) (speed * delta), 1, 1);

            r = Color.toRGBAR(c);
            g = Color.toRGBAG(c);
            b = Color.toRGBAB(c);
        }
        return this;
    }

    public RainbowColor set(RainbowColor color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        this.speed = color.speed;
        return this;
    }
}
