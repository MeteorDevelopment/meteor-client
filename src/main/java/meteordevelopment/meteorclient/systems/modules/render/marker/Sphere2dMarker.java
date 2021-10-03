/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Sphere2dMarker extends BaseMarker {
    public static final String type = "Sphere-2d";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgKeybinding = settings.createGroup("Keybinding");

    private final Setting<BlockPos> center = sgGeneral.add(new BlockPosSetting.Builder()
        .name("center")
        .description("Center of the sphere")
        .onChanged(bp -> calcCircle())
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Radius of the sphere")
        .defaultValue(20)
        .min(1)
        .noSlider(true)
        .onChanged(r -> calcCircle())
        .build()
    );

    private final Setting<Integer> layer = sgGeneral.add(new IntSetting.Builder()
        .name("layer")
        .description("Which layer to render")
        .defaultValue(0)
        .min(0)
        .noSlider(true)
        .onChanged(l -> calcCircle())
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
        .defaultValue(3)
        .min(1).sliderMax(10)
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
        .build()
    );

    private final Setting<Keybind> prevLayerKey = sgKeybinding.add(new KeybindSetting.Builder()
        .name("prev-layer-keybind")
        .description("Keybind to increment layer")
        .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final List<BlockPos> blocks = new ArrayList<>();

    public Sphere2dMarker() {
        super(type);
    }

    @Override
    protected void render(Render3DEvent event) {
        for (BlockPos blockPos : blocks) {
            if (!limitRenderRange.get() || PlayerUtils.distanceTo(blockPos) <= renderRange.get()) {
                event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    @Override
    protected void onKeyRelease(int key) {
        if (nextLayerKey.get().matches(true, key)) layer.set(layer.get() + 1);
        else if (prevLayerKey.get().matches(true, key)) layer.set(layer.get() - 1);
    }

    @Override
    public String getTypeName() {
        return type;
    }

    private void calcCircle() {
        blocks.clear();

        int cX = center.get().getX();
        int cY = center.get().getY();
        int cZ = center.get().getZ();

        int rSq = radius.get() * radius.get();
        int dY = -radius.get() + layer.get();

        // Calculate 1 octant and transform,mirror,flip the rest
        int dX = 0;
        while (true) {
            int dZ = (int) Math.round(Math.sqrt(rSq - (dX * dX + dY * dY)));

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


            // Stop when we reach the midpoint
            if (dX >= dZ) break;
            dX++;
        }
    }

    private void add(int x, int y, int z) {
        blocks.add(blockPos.set(x, y, z).toImmutable());
    }
}
