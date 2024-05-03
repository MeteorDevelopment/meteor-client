/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class BlockSelection extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
        .name("advanced")
        .description("Shows a more advanced outline on different types of shape blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> oneSide = sgGeneral.add(new BoolSetting.Builder()
        .name("single-side")
        .description("Only renders the side you are looking at.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Boolean> hideInside = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-when-inside-block")
        .description("Hide selection when inside target block.")
        .defaultValue(true)
        .build()
    );

    public BlockSelection() {
        super(Categories.Render, "block-selection", "Modifies how your block selection is rendered.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult result) || result.getType() == HitResult.Type.MISS) return;

        if (hideInside.get() && result.isInsideBlock()) return;

        BlockPos bp = result.getBlockPos();
        Direction side = result.getSide();

        VoxelShape shape = mc.world.getBlockState(bp).getOutlineShape(mc.world, bp);

        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox();

        if (oneSide.get()) {
            if (side == Direction.UP || side == Direction.DOWN) {
                event.renderer.sideHorizontal(bp.getX() + box.minX, bp.getY() + (side == Direction.DOWN ? box.minY : box.maxY), bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get());
            }
            else if (side == Direction.SOUTH || side == Direction.NORTH) {
                double z = side == Direction.NORTH ? box.minZ : box.maxZ;
                event.renderer.sideVertical(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + z, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + z, sideColor.get(), lineColor.get(), shapeMode.get());
            }
            else {
                double x = side == Direction.WEST ? box.minX : box.maxX;
                event.renderer.sideVertical(bp.getX() + x, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + x, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get());
            }
        }
        else {
            if (advanced.get()) {
                if (shapeMode.get() == ShapeMode.Both || shapeMode.get() == ShapeMode.Lines) {
                    shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        event.renderer.line(bp.getX() + minX, bp.getY() + minY, bp.getZ() + minZ, bp.getX() + maxX, bp.getY() + maxY, bp.getZ() + maxZ, lineColor.get());
                    });
                }

                if (shapeMode.get() == ShapeMode.Both || shapeMode.get() == ShapeMode.Sides) {
                    for (Box b : shape.getBoundingBoxes()) {
                        render(event, bp, b);
                    }
                }
            }
            else {
                render(event, bp, box);
            }
        }
    }

    private void render(Render3DEvent event, BlockPos bp, Box box) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
