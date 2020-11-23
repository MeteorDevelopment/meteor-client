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
    public enum Mode {
        Lines,
        Sides,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode")
            .defaultValue(Mode.Lines)
            .build()
    );

    private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
            .name("advanced")
            .description("Shows more advanced outline.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(255, 255, 255))
            .build()
    );

    private final Color sideColor = new Color();

    public BlockSelection() {
        super(Category.Render, "block-selection", "Modifies your block selection outline.");
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
                if (mode.get() == Mode.Lines || mode.get() == Mode.Both) {
                    ShapeBuilder.boxEdges(pos.getX() + minX, pos.getY() + d, pos.getZ() + e, pos.getX() + f, pos.getY() + g, pos.getZ() + h, color.get());
                }
                if (mode.get() == Mode.Sides || mode.get() == Mode.Both) {
                    setSideColor();
                    ShapeBuilder.boxSides(pos.getX() + minX, pos.getY() + d, pos.getZ() + e, pos.getX() + f, pos.getY() + g, pos.getZ() + h, sideColor);
                }
            });
        } else {
            if (mode.get() == Mode.Lines || mode.get() == Mode.Both) {
                ShapeBuilder.boxEdges(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, color.get());
            }
            if (mode.get() == Mode.Sides || mode.get() == Mode.Both) {
                setSideColor();
                ShapeBuilder.boxSides(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ, pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ, sideColor);
            }
        }
    });

    private void setSideColor() {
        sideColor.set(color.get());
        sideColor.a = 30;
    }
}
