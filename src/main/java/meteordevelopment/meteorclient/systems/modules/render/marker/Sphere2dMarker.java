/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Sphere2dMarker extends BaseMarker {
    private static class Block {
        public final int x, y, z;
        public int excludeDir;

        public Block(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final String type = "Sphere-2D";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgKeybinding = settings.createGroup("Keybinding");

    private final Setting<BlockPos> center = sgGeneral.add(new BlockPosSetting.Builder()
        .name("center")
        .description("Center of the sphere")
        .onChanged(bp -> dirty = true)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Radius of the sphere")
        .defaultValue(20)
        .min(1)
        .noSlider()
        .onChanged(r -> dirty = true)
        .build()
    );

    private final Setting<Integer> layer = sgGeneral.add(new IntSetting.Builder()
        .name("layer")
        .description("Which layer to render")
        .defaultValue(0)
        .min(0)
        .noSlider()
        .onChanged(l -> dirty = true)
        .build()
    );

    // Render

    private final Setting<Boolean> limitRenderRange = sgRender.add(new BoolSetting.Builder()
        .name("limit-render-range")
        .description("Whether to limit rendering range (useful in very large circles)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> renderRange = sgRender.add(new IntSetting.Builder()
        .name("render-range")
        .description("Rendering range")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 20)
        .visible(limitRenderRange::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(0, 100, 255, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(0, 100, 255, 255))
        .build()
    );

    // Keybinding

    private final Setting<Keybind> nextLayerKey = sgKeybinding.add(new KeybindSetting.Builder()
        .name("next-layer-keybind")
        .description("Keybind to increment layer")
        .action(() -> {
            if (isVisible() && layer.get() < radius.get() * 2) layer.set(layer.get() + 1);
        })
        .build()
    );

    private final Setting<Keybind> prevLayerKey = sgKeybinding.add(new KeybindSetting.Builder()
        .name("prev-layer-keybind")
        .description("Keybind to increment layer")
        .action(() -> {
            if (isVisible()) layer.set(layer.get() - 1);
        })
        .build()
    );

    private final List<Block> blocks = new ArrayList<>();
    private boolean dirty = true, calculating;

    public Sphere2dMarker() {
        super(type);
    }

    @Override
    protected void render(Render3DEvent event) {
        if (dirty && !calculating) calcCircle();

        synchronized (blocks) {
            for (Block block : blocks) {
                if (!limitRenderRange.get() || PlayerUtils.distanceTo(block.x, block.y, block.z) <= renderRange.get()) {
                    event.renderer.box(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), block.excludeDir);
                }
            }
        }
    }

    @Override
    public String getTypeName() {
        return type;
    }

    private void calcCircle() {
        calculating = true;
        blocks.clear();

        Runnable action = () -> {
            int cX = center.get().getX();
            int cY = center.get().getY();
            int cZ = center.get().getZ();

            int rSq = radius.get() * radius.get();
            int dY = -radius.get() + layer.get();

            // Calculate 1 octant and transform,mirror,flip the rest
            int dX = 0;
            while (true) {
                int dZ = (int) Math.round(Math.sqrt(rSq - (dX * dX + dY * dY)));

                synchronized (blocks) {
                    // First and second octant
                    add(cX + dX, cY + dY, cZ + dZ);
                    add(cX + dZ, cY + dY, cZ + dX);

                    // Fifth and sixth octant
                    add(cX - dX, cY + dY, cZ - dZ);
                    add(cX - dZ, cY + dY, cZ - dX);

                    // Third and fourth octant
                    add(cX + dX, cY + dY, cZ - dZ);
                    add(cX + dZ, cY + dY, cZ - dX);

                    // Seventh and eighth octant
                    add(cX - dX, cY + dY, cZ + dZ);
                    add(cX - dZ, cY + dY, cZ + dX);
                }


                // Stop when we reach the midpoint
                if (dX >= dZ) break;
                dX++;
            }

            // Calculate connected blocks
            synchronized (blocks) {
                for (Block block : blocks) {
                    for (Block b : blocks) {
                        if (b == block) continue;

                        if (b.x == block.x + 1 && b.z == block.z) block.excludeDir |= Dir.EAST;
                        if (b.x == block.x - 1 && b.z == block.z) block.excludeDir |= Dir.WEST;
                        if (b.x == block.x && b.z == block.z + 1) block.excludeDir |= Dir.SOUTH;
                        if (b.x == block.x && b.z == block.z - 1) block.excludeDir |= Dir.NORTH;
                    }
                }
            }

            dirty = false;
            calculating = false;
        };

        if (radius.get() <= 50) action.run();
        else MeteorExecutor.execute(action);
    }

    private void add(int x, int y, int z) {
        for (Block b : blocks) {
            if (b.x == x && b.y == y && b.z == z) return;
        }

        blocks.add(new Block(x, y, z));
    }
}
