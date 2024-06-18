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

import java.util.HashMap;
import java.util.List;

public class Sphere3dMarker extends BaseMarker {
    private static class Block {
        public final int x, y, z;
        public int excludeDir;

        public Block(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final String type = "Sphere-3D";

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
        .sliderRange(1, 64)
        .onChanged(r -> dirty = true)
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
        .sliderRange(1, 128)
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

    private final HashMap<String, Block> blocks = new HashMap<>();
    private boolean dirty = true, calculating;

    public Sphere3dMarker() {
        super(type);
    }

    @Override
    protected void render(Render3DEvent event) {
        if (dirty && !calculating) calcCircle();

        synchronized (blocks) {
            for (Block block : blocks.values()) {
                if (!limitRenderRange.get() || PlayerUtils.isWithin(block.x, block.y, block.z, renderRange.get())) {
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

            int r = radius.get();
            int rSq = r * r;
            int d = r * 2;

            
            for (int slice = 0; slice < d; slice++) {
                int dY = -r + slice;

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
            }

            // Calculate connected blocks
            synchronized (blocks) {
                for (Block block : blocks.values()) {
                    int x = block.x;
                    int y = block.y;
                    int z = block.z;

                    String east = String.format("%d,%d,%d", x + 1, y, z);
                    String west = String.format("%d,%d,%d", x - 1, y, z);
                    String south = String.format("%d,%d,%d", x, y, z + 1);
                    String north = String.format("%d,%d,%d", x, y, z - 1);
                    String top = String.format("%d,%d,%d", x, y + 1, z);
                    String bottom = String.format("%d,%d,%d", x, y - 1, z);

                    if (blocks.containsKey(east)) block.excludeDir |= Dir.EAST;
                    if (blocks.containsKey(west)) block.excludeDir |= Dir.WEST;
                    if (blocks.containsKey(south)) block.excludeDir |= Dir.SOUTH;
                    if (blocks.containsKey(north)) block.excludeDir |= Dir.NORTH;
                    if (blocks.containsKey(top)) block.excludeDir |= Dir.UP;
                    if (blocks.containsKey(bottom)) block.excludeDir |= Dir.DOWN;
                }
            }

            dirty = false;
            calculating = false;
        };

        if (radius.get() <= 50) action.run();
        else MeteorExecutor.execute(action);
    }

    private void add(int x, int y, int z) {
        blocks.putIfAbsent(String.format("%d,%d,%d", x, y, z), new Block(x, y, z));
    }
}
