/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;

// TODO: Add outline and more modes
public class CuboidMarker extends BaseMarker {
    public static final String type = "Cuboid";

    public enum Mode {
        Full
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<BlockPos> pos1 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("pos-1")
        .description("1st corner of the cuboid")
        .onChanged(bp -> onChanged())
        .build()
    );

    private final Setting<BlockPos> pos2 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("pos-2")
        .description("2nd corner of the cuboid")
        .onChanged(bp -> onChanged())
        .build()
    );

    private final Setting<Mode> mode = sgRender.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What mode to use for this marker.")
        .defaultValue(Mode.Full)
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

    private static final BlockPos.Mutable bp = new BlockPos.Mutable();
    private int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;

    public CuboidMarker() {
        super(type);
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    protected void render(Render3DEvent event) {
        switch (mode.get()) {
            case Full    : renderFull(event);
        }
    }

    private void renderFull(Render3DEvent event) {
        // Edges
        if (shapeMode.get() == ShapeMode.Lines || shapeMode.get() == ShapeMode.Both) {
            event.renderer.line(minX, minY, minZ, maxX + 1, minY, minZ, lineColor.get());
            event.renderer.line(minX, minY, minZ, minX, maxY + 1, minZ, lineColor.get());
            event.renderer.line(minX, minY, minZ, minX, minY, maxZ + 1, lineColor.get());

            event.renderer.line(maxX + 1, minY, minZ, maxX + 1, minY, maxZ + 1, lineColor.get());
            event.renderer.line(maxX + 1, minY, minZ, maxX + 1, maxY + 1, minZ, lineColor.get());

            event.renderer.line(minX, maxY + 1, minZ, minX, maxY + 1, maxZ + 1, lineColor.get());
            event.renderer.line(minX, maxY + 1, minZ, maxX + 1, maxY + 1, minZ, lineColor.get());

            event.renderer.line(minX, minY, maxZ + 1, minX, maxY + 1, maxZ + 1, lineColor.get());
            event.renderer.line(minX, minY, maxZ + 1, maxX + 1, minY, maxZ + 1, lineColor.get());

            event.renderer.line(maxX + 1, maxY + 1, maxZ + 1, minX, maxY + 1, maxZ + 1, lineColor.get());
            event.renderer.line(maxX + 1, maxY + 1, maxZ + 1, maxX + 1, minY, maxZ + 1, lineColor.get());
            event.renderer.line(maxX + 1, maxY + 1, maxZ + 1, maxX + 1, maxY + 1, minZ, lineColor.get());
        }

        // Faces
        if (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) {
            event.renderer.quad(minX, minY, minZ, maxX + 1, minY, minZ, maxX + 1, maxY + 1, minZ, minX, maxY + 1, minZ, sideColor.get());
            event.renderer.quad(minX, minY, minZ, minX, minY, maxZ + 1, minX, maxY + 1, maxZ + 1, minX, maxY + 1, maxZ + 1, sideColor.get());
            event.renderer.quad(minX, minY, minZ, maxX + 1, minY, minZ, maxX + 1, minY, maxZ + 1, minX, minY, maxZ + 1, sideColor.get());

            event.renderer.quad(maxX + 1, maxY + 1, maxZ + 1, minX, maxY + 1, maxZ + 1, minX, minY, maxZ + 1, maxX + 1, minY, maxZ + 1, sideColor.get());
            event.renderer.quad(maxX + 1, maxY + 1, maxZ + 1, maxX + 1, maxY + 1, minZ, maxX + 1, minY, minZ, maxX + 1, minY, maxZ + 1, sideColor.get());
            event.renderer.quad(maxX + 1, maxY + 1, maxZ + 1, minX, maxY + 1, maxZ + 1, minX, maxY + 1, minZ, maxX + 1, maxY + 1, minZ, sideColor.get());
        }
    }

    private void onChanged() {
        minX = Math.min(pos1.get().getX(), pos2.get().getX());
        maxX = Math.max(pos1.get().getX(), pos2.get().getX());
        minY = Math.min(pos1.get().getY(), pos2.get().getY());
        maxY = Math.max(pos1.get().getY(), pos2.get().getY());
        minZ = Math.min(pos1.get().getZ(), pos2.get().getZ());
        maxZ = Math.max(pos1.get().getZ(), pos2.get().getZ());
    }
}
