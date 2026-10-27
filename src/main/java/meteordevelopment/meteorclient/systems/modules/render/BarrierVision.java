/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BarrierVision extends Module {
    private static final List<Block> TARGETS = List.of(Blocks.BARRIER, Blocks.LIGHT, Blocks.STRUCTURE_VOID);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The block search range (in blocks).")

        .defaultValue(24)
        .min(1)
        .sliderRange(5, 48)
        .build()
    );

    private int timer = 0;

    public BarrierVision() {
        super(Categories.Render, "barrier-vision", "Makes invisible visible.");
    }

    @Override
    public void onActivate() {
        timer = Integer.MAX_VALUE / 2;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;

        if (++timer < 40) return;
        timer = 0;

        int r = range.get();
        int spawned = 0;

        BlockPos center = mc.player.blockPosition();
        int minY = Math.max(center.getY() - r, mc.level.getMinY());
        int maxY = Math.min(center.getY() + r, mc.level.getMinY() + mc.level.getHeight() - 1);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);

                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir() || !TARGETS.contains(state.getBlock())) continue;

                    mc.level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                        x + 0.5, y + 0.5, z + 0.5,
                        0, 0, 0
                    );

                    if (++spawned >= 5000) return;
                }
            }
        }
    }
}
