/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class LightOverlay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Integer> horizontalRange = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-range")
            .description("Horizontal range in blocks.")
            .defaultValue(8)
            .min(0)
            .build()
    );

    private final Setting<Integer> verticalRange = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-range")
            .description("Vertical range in blocks.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Boolean> seeThroughBlocks = sgGeneral.add(new BoolSetting.Builder()
            .name("see-through-blocks")
            .description("Allows you to see the lines through blocks.")
            .defaultValue(false)
            .build()
    );

    // Colors

    private final Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
            .name("color")
            .description("Color of places where mobs can currently spawn.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    private final Setting<SettingColor> potentialColor = sgColors.add(new ColorSetting.Builder()
            .name("potential-color")
            .description("Color of places where mobs can potentially spawn (eg at night).")
            .defaultValue(new SettingColor(225, 225, 25))
            .build()
    );

    private final Pool<Cross> crossPool = new Pool<>(Cross::new);
    private final List<Cross> crosses = new ArrayList<>();

    private final BlockPos.Mutable bp = new BlockPos.Mutable();

    private final MeshBuilder mb = new MeshBuilder();

    public LightOverlay() {
        super(Categories.Render, "light-overlay", "Shows blocks where mobs can spawn.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Cross cross : crosses) crossPool.free(cross);
        crosses.clear();

        BlockIterator.register(horizontalRange.get(), verticalRange.get(), (blockPos, blockState) -> {
            if (!blockState.getCollisionShape(mc.world, blockPos).isEmpty()) return;

            bp.set(blockPos).move(0, -1, 0);
            if (mc.world.getBlockState(bp).getCollisionShape(mc.world, bp) != VoxelShapes.fullCube()) return;

            if (mc.world.getLightLevel(blockPos, 0) <= 7) {
                crosses.add(crossPool.get().set(blockPos, false));
            }
            else if (mc.world.getLightLevel(LightType.BLOCK, blockPos) <= 7) {
                crosses.add(crossPool.get().set(blockPos, true));
            }
        });
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (crosses.isEmpty()) return;

        mb.depthTest = !seeThroughBlocks.get();
        mb.begin(event, DrawMode.Lines, VertexFormats.POSITION_COLOR);

        for (Cross cross : crosses) cross.render();

        mb.end();
    }

    private class Cross {
        private double x, y, z;
        private boolean potential;

        public Cross set(BlockPos blockPos, boolean potential) {
            x = blockPos.getX();
            y = blockPos.getY() + 0.0075;
            z = blockPos.getZ();

            this.potential = potential;

            return this;
        }

        public void render() {
            Color c = potential ? potentialColor.get() : color.get();

            mb.line(x, y, z, x + 1, y, z + 1, c);
            mb.line(x + 1, y, z, x, y, z + 1, c);
        }
    }
}
