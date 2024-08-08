/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import net.minecraft.util.math.BlockPos;

import java.util.Set;

public abstract class AbstractSphereMarker extends BaseMarker {
    public AbstractSphereMarker(String name) {
        super(name);
    }

    protected static void computeCircle(Set<RenderBlock> renderBlocks, BlockPos center, int dY, int radius) {
        int cX = center.getX();
        int cY = center.getY();
        int cZ = center.getZ();

        int rSq = radius * radius;

        // Calculate 1 octant and transform,mirror,flip the rest
        int dZ = 1;
        for (int dX = 0; dX < dZ; dX++) {
            dZ = (int) Math.round(Math.sqrt(rSq - (dX * dX + dY * dY)));

            // First and second octant
            renderBlocks.add(new RenderBlock(cX + dX, cY + dY, cZ + dZ));
            renderBlocks.add(new RenderBlock(cX + dZ, cY + dY, cZ + dX));

            // Fifth and sixth octant
            renderBlocks.add(new RenderBlock(cX - dX, cY + dY, cZ - dZ));
            renderBlocks.add(new RenderBlock(cX - dZ, cY + dY, cZ - dX));

            // Third and fourth octant
            renderBlocks.add(new RenderBlock(cX + dX, cY + dY, cZ - dZ));
            renderBlocks.add(new RenderBlock(cX + dZ, cY + dY, cZ - dX));

            // Seventh and eighth octant
            renderBlocks.add(new RenderBlock(cX - dX, cY + dY, cZ + dZ));
            renderBlocks.add(new RenderBlock(cX - dZ, cY + dY, cZ + dX));
        }
    }
}
