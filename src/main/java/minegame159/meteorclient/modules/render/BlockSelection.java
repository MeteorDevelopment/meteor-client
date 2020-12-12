/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class BlockSelection extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
            .name("advanced")
            .description("Shows more advanced outline.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> fillColor = sgGeneral.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("Fill color.")
            .defaultValue(new Color(255, 255, 255, 0))
            .build()
    );

    private final Setting<Color> outlineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("outline-color")
            .description("Outline color.")
            .defaultValue(new Color(255, 255, 255, 255))
            .build()
    );

    private final Color sideColor = new Color();

    public BlockSelection() {
        super(Category.Render, "block-selection", "Modifies your block selection render.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (mc.crosshairTarget == null || !(mc.crosshairTarget instanceof BlockHitResult)) return;

        BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox();

        if (advanced.get()) {
            shape.forEachBox((minX, d, e, f, g, h) -> {
                ShapeBuilder.boxEdges(pos.getX() + minX, pos.getY() + d, pos.getZ() + e, pos.getX() + f, pos.getY() + g, pos.getZ() + h, outlineColor.get());
                ShapeBuilder.boxSides(pos.getX() + minX, pos.getY() + d, pos.getZ() + e, pos.getX() + f, pos.getY() + g, pos.getZ() + h, fillColor.get());
            });
        } else {
            ShapeBuilder.boxEdges(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, outlineColor.get());
            ShapeBuilder.boxSides(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, fillColor.get());
        }
    });
}
