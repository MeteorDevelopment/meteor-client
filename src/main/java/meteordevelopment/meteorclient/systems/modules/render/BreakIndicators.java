/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.LevelRendererAccessor;
import meteordevelopment.meteorclient.mixin.MultiPlayerGameModeAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class BreakIndicators extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Whether or not to render blocks being packet mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> startSideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("start-side-color")
        .description("The side color for the non-broken block.")
        .defaultValue(new SettingColor(25, 252, 25, 150))
        .visible(() -> shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> startLineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("start-line-color")
        .description("The line color for the non-broken block.")
        .defaultValue(new SettingColor(25, 252, 25, 150))
        .visible(() -> shapeMode.get().lines())
        .build()
    );

    private final Setting<SettingColor> endSideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("end-side-color")
        .description("The side color for the fully-broken block.")
        .defaultValue(new SettingColor(255, 25, 25, 150))
        .visible(() -> shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> endLineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("end-line-color")
        .description("The line color for the fully-broken block.")
        .defaultValue(new SettingColor(255, 25, 25, 150))
        .visible(() -> shapeMode.get().lines())
        .build()
    );

    private final Color cSides = new Color();
    private final Color cLines = new Color();

    public BreakIndicators() {
        super(Categories.Render, "break-indicators", "Renders the progress of a block being broken.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderNormal(event);

        if (packetMine.get() && !Modules.get().get(PacketMine.class).blocks.isEmpty()) {
            renderPacket(event, Modules.get().get(PacketMine.class).blocks);
        }

        HighwayBuilder b = Modules.get().get(HighwayBuilder.class);
        if (!b.isActive()) return;

        if (b.normalMining != null) {
            VoxelShape voxelShape = b.normalMining.blockState.getShape(mc.level, b.normalMining.blockPos);
            if (voxelShape.isEmpty()) return;

            double normalised = Math.min(1, b.normalMining.progress());
            renderBlock(event, voxelShape.bounds(), b.normalMining.blockPos, 1 - normalised, normalised);
        }

        if (b.packetMining != null) {
            VoxelShape voxelShape = b.packetMining.blockState.getShape(mc.level, b.packetMining.blockPos);
            if (voxelShape.isEmpty()) return;

            double normalised = Math.min(1, b.packetMining.progress());
            renderBlock(event, voxelShape.bounds(), b.packetMining.blockPos, 1 - normalised, normalised);
        }
    }

    private void renderNormal(Render3DEvent event) {
        Map<Integer, BlockDestructionProgress> blocks = ((LevelRendererAccessor) mc.levelRenderer).meteor$getDestroyingBlocks();

        float ownBreakingStage = ((MultiPlayerGameModeAccessor) mc.gameMode).meteor$getBreakingProgress();
        BlockPos ownBreakingPos = ((MultiPlayerGameModeAccessor) mc.gameMode).meteor$getCurrentBreakingBlockPos();

        if (ownBreakingPos != null && ownBreakingStage > 0) {
            BlockState state = mc.level.getBlockState(ownBreakingPos);
            VoxelShape shape = state.getShape(mc.level, ownBreakingPos);
            if (shape == null || shape.isEmpty()) return;

            AABB orig = shape.bounds();

            double shrinkFactor = 1d - ownBreakingStage;

            renderBlock(event, orig, ownBreakingPos, shrinkFactor, ownBreakingStage);
        }

        blocks.values().forEach(info -> {
            BlockPos pos = info.getPos();
            int stage = info.getProgress();
            if (pos.equals(ownBreakingPos)) return;

            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getShape(mc.level, pos);
            if (shape == null || shape.isEmpty()) return;

            AABB orig = shape.bounds();

            double shrinkFactor = (9 - (stage + 1)) / 9d;
            double progress = 1d - shrinkFactor;

            renderBlock(event, orig, pos, shrinkFactor, progress);
        });
    }

    private void renderPacket(Render3DEvent event, List<PacketMine.MyBlock> blocks) {
        for (PacketMine.MyBlock block : blocks) {
            if (block.mining && block.progress() != Double.POSITIVE_INFINITY) {
                VoxelShape shape = block.blockState.getShape(mc.level, block.blockPos);
                if (shape == null || shape.isEmpty()) return;

                AABB orig = shape.bounds();

                double progressNormalised = Math.min(1, block.progress());
                double shrinkFactor = 1d - progressNormalised;
                BlockPos pos = block.blockPos;

                renderBlock(event, orig, pos, shrinkFactor, progressNormalised);
            }
        }
    }

    private void renderBlock(Render3DEvent event, AABB orig, BlockPos pos, double shrinkFactor, double progress) {
        AABB box = orig.contract(
            orig.getXsize() * shrinkFactor,
            orig.getYsize() * shrinkFactor,
            orig.getZsize() * shrinkFactor
        );

        double xShrink = (orig.getXsize() * shrinkFactor) / 2;
        double yShrink = (orig.getYsize() * shrinkFactor) / 2;
        double zShrink = (orig.getZsize() * shrinkFactor) / 2;

        double x1 = pos.getX() + box.minX + xShrink;
        double y1 = pos.getY() + box.minY + yShrink;
        double z1 = pos.getZ() + box.minZ + zShrink;
        double x2 = pos.getX() + box.maxX + xShrink;
        double y2 = pos.getY() + box.maxY + yShrink;
        double z2 = pos.getZ() + box.maxZ + zShrink;

        Color c1Sides = startSideColor.get().copy().a(startSideColor.get().a / 2);
        Color c2Sides = endSideColor.get().copy().a(endSideColor.get().a / 2);

        cSides.set(
            (int) Math.round(c1Sides.r + (c2Sides.r - c1Sides.r) * progress),
            (int) Math.round(c1Sides.g + (c2Sides.g - c1Sides.g) * progress),
            (int) Math.round(c1Sides.b + (c2Sides.b - c1Sides.b) * progress),
            (int) Math.round(c1Sides.a + (c2Sides.a - c1Sides.a) * progress)
        );

        Color c1Lines = startLineColor.get();
        Color c2Lines = endLineColor.get();

        cLines.set(
            (int) Math.round(c1Lines.r + (c2Lines.r - c1Lines.r) * progress),
            (int) Math.round(c1Lines.g + (c2Lines.g - c1Lines.g) * progress),
            (int) Math.round(c1Lines.b + (c2Lines.b - c1Lines.b) * progress),
            (int) Math.round(c1Lines.a + (c2Lines.a - c1Lines.a) * progress)
        );

        event.renderer.box(x1, y1, z1, x2, y2, z2, cSides, cLines, shapeMode.get(), 0);
    }
}
