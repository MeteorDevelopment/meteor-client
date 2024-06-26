/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.packer;

import java.util.ArrayList;
import java.util.List;

public class GuiTexture {
    private final List<TextureRegion> regions = new ArrayList<>(2);

    void add(TextureRegion region) {
        regions.add(region);
    }

    public TextureRegion get(double width, double height) {
        double targetDiagonal = Math.sqrt(width * width + height * height);

        double closestDifference = Double.MAX_VALUE;
        TextureRegion closestRegion = null;

        for (TextureRegion region : regions) {
            double difference = Math.abs(targetDiagonal - region.diagonal);

            if (difference < closestDifference) {
                closestDifference = difference;
                closestRegion = region;
            }
        }

        return closestRegion;
    }
}
