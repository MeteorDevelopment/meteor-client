/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
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
            .name("one-side")
            .description("Renders only the side you are looking at.")
            .defaultValue(false)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
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

    public BlockSelection() {
        super(Categories.Render, "block-selection", "Modifies how your block selection is rendered.");
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult)) return;

        BlockHitResult result = (BlockHitResult) mc.crosshairTarget;

        BlockPos bp = result.getBlockPos();
        Direction side = result.getSide();

        BlockState state = mc.world.getBlockState(bp);
        VoxelShape shape = state.getOutlineShape(mc.world, bp);

        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox();

        if (oneSide.get()) {
            if (side == Direction.UP || side == Direction.DOWN) {
                Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, bp.getX() + box.minX, bp.getY() + (side == Direction.DOWN ? box.minY : box.maxY), bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get());
            }
            else if (side == Direction.SOUTH || side == Direction.NORTH) {
                double z = side == Direction.NORTH ? box.minZ : box.maxZ;
                Renderer.quadWithLinesVertical(Renderer.NORMAL, Renderer.LINES, bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + z, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + z, sideColor.get(), lineColor.get(), shapeMode.get());
            }
            else {
                double x = side == Direction.WEST ? box.minX : box.maxX;
                Renderer.quadWithLinesVertical(Renderer.NORMAL, Renderer.LINES, bp.getX() + x, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + x, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get());
            }
        }
        else {
            if (advanced.get()) {
                for (Box b : shape.getBoundingBoxes()) {
                    render(bp, b);
                }
            }
            else {
                render(bp, box);
            }
        }
    }

    private void render(BlockPos bp, Box box) {
        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
