/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class HoleHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<List<Block>> safe = sgGeneral.add(new BlockListSetting.Builder()
        .name("safe-blocks")
        .description("Which blocks to consider safe.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.BEDROCK, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Color BG_COLOR = new Color(255, 25, 25, 100);
    private final Color OL_COLOR = new Color(255, 25, 25, 255);

    public HoleHud(HUD hud) {
        super(hud, "hole", "Displays information about the hole you are standing in.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * 3 * scale.get(), 16 * 3 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        Renderer2D.COLOR.begin();

        drawBlock(get(Facing.Left), x, y + 16 * scale.get()); // Left
        drawBlock(get(Facing.Front), x + 16 * scale.get(), y); // Front
        drawBlock(get(Facing.Right), x + 32 * scale.get(), y + 16 * scale.get()); // Right
        drawBlock(get(Facing.Back), x + 16 * scale.get(), y + 32 * scale.get()); // Back

        Renderer2D.COLOR.render(null);
    }

    private Direction get(Facing dir) {
        if (!Utils.canUpdate() || isInEditor()) return Direction.DOWN;
        return Direction.fromRotation(MathHelper.wrapDegrees(mc.player.getYaw() + dir.offset));
    }

    private void drawBlock(Direction dir, double x, double y) {
        Block block = dir == Direction.DOWN ? Blocks.OBSIDIAN : mc.world.getBlockState(mc.player.getBlockPos().offset(dir)).getBlock();
        if (!safe.get().contains(block)) return;

        RenderUtils.drawItem(block.asItem().getDefaultStack(), (int) x, (int) y, scale.get(), false);

        if (dir == Direction.DOWN) return;

        ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values().forEach(info -> {
            if (info.getPos().equals(mc.player.getBlockPos().offset(dir))) {
                renderBreaking(x, y, info.getStage() / 9f);
            }
        });
    }

    private void renderBreaking(double x, double y, double percent) {
        Renderer2D.COLOR.quad(x, y, (16 * percent) * scale.get(), 16 * scale.get(), BG_COLOR);
        Renderer2D.COLOR.quad(x, y, 16 * scale.get(), 1 * scale.get(), OL_COLOR);
        Renderer2D.COLOR.quad(x, y + 15 * scale.get(), 16 * scale.get(), 1 * scale.get(), OL_COLOR);
        Renderer2D.COLOR.quad(x, y, 1 * scale.get(), 16 * scale.get(), OL_COLOR);
        Renderer2D.COLOR.quad(x + 15 * scale.get(), y, 1 * scale.get(), 16 * scale.get(), OL_COLOR);
    }

    private enum Facing {
        Left(-90),
        Right(90),
        Front(0),
        Back(180);

        int offset;

        Facing(int offset) {
            this.offset = offset;
        }
    }
}
