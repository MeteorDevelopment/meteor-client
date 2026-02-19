/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Future;

public class CylinderMarker extends AbstractSphereMarker {
    public static final String type = "Cylinder";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

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

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("The height of the cylinder")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 20)
        .onChanged(l -> dirty = true)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What mode to use for this marker.")
        .defaultValue(Mode.Hollow)
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

    private volatile List<RenderBlock> blocks = List.of();
    private volatile @Nullable Future<?> task = null;
    private boolean dirty = true;

    public CylinderMarker() {
        super(type);
    }

    @Override
    protected void render(Render3DEvent event) {
        if (dirty) calcCircle();

        for (RenderBlock block : blocks) {
            if (!limitRenderRange.get() || PlayerUtils.isWithin(block.x, block.y, block.z, renderRange.get())) {
                event.renderer.box(block.x, block.y, block.z, block.x + 1, block.y + height.get(), block.z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), block.excludeDir);
            }
        }
    }

    @Override
    public String getTypeName() {
        return type;
    }

    private void calcCircle() {
        dirty = false;

        if (task != null) {
            task.cancel(true);
            task = null;
        }

        Runnable action = () -> {
            blocks = switch (mode.get()) {
                case Full -> filledCircle(center.get(), radius.get());
                case Hollow -> hollowCircle(center.get(), radius.get());
            };
            task = null;
        };

        if (radius.get() <= 50) action.run();
        else {
            task = MeteorExecutor.executeFuture(action);
        }
    }

    private static List<RenderBlock> hollowCircle(BlockPos center, int r) {
        ObjectOpenHashSet<RenderBlock> renderBlocks = new ObjectOpenHashSet<>();

        computeCircle(renderBlocks, center, 0, r);
        cullInnerFaces(renderBlocks);

        return new ObjectArrayList<>(renderBlocks);
    }

    private static List<RenderBlock> filledCircle(BlockPos center, int r) {
        ObjectOpenHashSet<RenderBlock> renderBlocks = new ObjectOpenHashSet<>();

        int rSq = r * r;

        for (int dX = 0; dX <= r; dX++) {
            int dXSq = dX * dX;
            for (int dZ = 0; dZ <= r; dZ++) {
                int dZSq = dZ * dZ;
                if (dXSq + dZSq <= rSq) {
                    renderBlocks.add(new RenderBlock(center.getX() + dX, center.getY(), center.getZ() + dZ));
                    renderBlocks.add(new RenderBlock(center.getX() - dX, center.getY(), center.getZ() + dZ));
                    renderBlocks.add(new RenderBlock(center.getX() + dX, center.getY(), center.getZ() - dZ));
                    renderBlocks.add(new RenderBlock(center.getX() - dX, center.getY(), center.getZ() - dZ));
                }
            }
        }

        cullInnerFaces(renderBlocks);

        return new ObjectArrayList<>(renderBlocks);
    }

    private static void cullInnerFaces(ObjectOpenHashSet<RenderBlock> renderBlocks) {
        for (RenderBlock block : renderBlocks) {
            int x = block.x;
            int y = block.y;
            int z = block.z;

            @Nullable RenderBlock east = renderBlocks.get(new RenderBlock(x + 1, y, z));
            if (east != null) {
                block.excludeDir |= Dir.EAST;
                east.excludeDir |= Dir.WEST;
            }

            @Nullable RenderBlock south = renderBlocks.get(new RenderBlock(x, y, z + 1));
            if (south != null) {
                block.excludeDir |= Dir.SOUTH;
                south.excludeDir |= Dir.NORTH;
            }
        }
    }
}
