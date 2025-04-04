/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.renderer.packer;

public class TextureRegion {
    public double x1, y1;
    public double x2, y2;

    public double diagonal;

    public TextureRegion(double width, double height) {
        diagonal = Math.sqrt(width * width + height * height);
    }
}
