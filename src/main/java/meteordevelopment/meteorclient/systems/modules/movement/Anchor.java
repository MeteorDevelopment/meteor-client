/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class Anchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
        .name("max-height")
        .description("The maximum height Anchor will work at.")
        .defaultValue(10)
        .range(0, 255)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> minPitch = sgGeneral.add(new IntSetting.Builder()
        .name("min-pitch")
        .description("The minimum pitch at which anchor will work.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .build()
    );

    private final Setting<Boolean> cancelMove = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-jump-in-hole")
        .description("Prevents you from jumping when Anchor is active and Min Pitch is met.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pull = sgGeneral.add(new BoolSetting.Builder()
        .name("pull")
        .description("The pull strength of Anchor.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> pullSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("pull-speed")
        .description("How fast to pull towards the hole in blocks per second.")
        .defaultValue(0.3)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private boolean wasInHole;
    private boolean foundHole;
    private int holeX, holeZ;

    public boolean cancelJump;

    public boolean controlMovement;
    public double deltaX, deltaZ;

    public Anchor() {
        super(Categories.Movement, "anchor", "Helps you get into holes by stopping your movement completely over a hole.");
    }

    @Override
    public void onActivate() {
        wasInHole = false;
        holeX = holeZ = 0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        cancelJump = foundHole && cancelMove.get() && mc.player.getPitch() >= minPitch.get();
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        controlMovement = false;

        int x = MathHelper.floor(mc.player.getX());
        int y = MathHelper.floor(mc.player.getY());
        int z = MathHelper.floor(mc.player.getZ());

        if (isHole(x, y, z)) {
            wasInHole = true;
            holeX = x;
            holeZ = z;
            return;
        }

        if (wasInHole && holeX == x && holeZ == z) return;
        else if (wasInHole) wasInHole = false;

        if (mc.player.getPitch() < minPitch.get()) return;

        foundHole = false;
        double holeX = 0;
        double holeZ = 0;

        for (int i = 0; i < maxHeight.get(); i++) {
            y--;
            if (y <= 0 || !isAir(x, y, z)) break;

            if (isHole(x, y, z)) {
                foundHole = true;
                holeX = x + 0.5;
                holeZ = z + 0.5;
                break;
            }
        }

        if (foundHole) {
            controlMovement = true;
            deltaX = Utils.clamp(holeX - mc.player.getX(), -0.05, 0.05);
            deltaZ = Utils.clamp(holeZ - mc.player.getZ(), -0.05, 0.05);

            ((IVec3d) mc.player.getVelocity()).set(deltaX, mc.player.getVelocity().y - (pull.get() ? pullSpeed.get() : 0), deltaZ);
        }
    }

    private boolean isHole(int x, int y, int z) {
        return isHoleBlock(x, y - 1, z) &&
                isHoleBlock(x + 1, y, z) &&
                isHoleBlock(x - 1, y, z) &&
                isHoleBlock(x, y, z + 1) &&
                isHoleBlock(x, y, z - 1);
    }

    private boolean isHoleBlock(int x, int y, int z) {
        blockPos.set(x, y, z);
        Block block = mc.world.getBlockState(blockPos).getBlock();
        return block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN;
    }

    private boolean isAir(int x, int y, int z) {
        blockPos.set(x, y, z);
        return !((AbstractBlockAccessor)mc.world.getBlockState(blockPos).getBlock()).isCollidable();
    }
}
