/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("shape")
        .description("The shape of nuking algorithm.")
        .defaultValue(Shape.Sphere)
        .build()
    );

    private final Setting<Nuker.Mode> mode = sgGeneral.add(new EnumSetting.Builder<Nuker.Mode>()
        .name("mode")
        .description("The way the blocks are broken.")
        .defaultValue(Nuker.Mode.Flatten)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The break range.")
        .defaultValue(4)
        .min(0)
        .visible(() -> shape.get() != Shape.Cube)
        .build()
    );


    private final Setting<Integer> range_up = sgGeneral.add(new IntSetting.Builder()
        .name("up")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_down = sgGeneral.add(new IntSetting.Builder()
        .name("down")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_left = sgGeneral.add(new IntSetting.Builder()
        .name("left")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_right = sgGeneral.add(new IntSetting.Builder()
        .name("right")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_forward = sgGeneral.add(new IntSetting.Builder()
        .name("forward")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> range_back = sgGeneral.add(new IntSetting.Builder()
        .name("back")
        .description("The break range.")
        .defaultValue(1)
        .min(0)
        .visible(() -> shape.get() == Shape.Cube)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to break per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Nuker.SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<Nuker.SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(Nuker.SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Attempt to instamine everything at once.")
        .defaultValue(false)
        .build()
    );

    // Whitelist

    private final Setting<Boolean> whitelistEnabled = sgWhitelist.add(new BoolSetting.Builder()
        .name("whitelist-enabled")
        .description("Only mines selected blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(whitelistEnabled::get)
        .build()
    );

    // Rendering

    // Bounding box
    private final Setting<Boolean> enableRenderBounding = sgRender.add(new BoolSetting.Builder()
        .name("bounding-box")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBox = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-box-mode")
        .description("How the shape for the bounding box is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColorBox = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 100))
        .build()
    );

    private final Setting<SettingColor> lineColorBox = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 255))
        .build()
    );

    // Broken blocks

    private final Setting<Boolean> enableRenderBreaking = sgRender.add(new BoolSetting.Builder()
        .name("broken-blocks")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBreak = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-block-mode")
        .description("How the shapes for broken blocks are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(enableRenderBreaking::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .visible(enableRenderBreaking::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(enableRenderBreaking::get)
        .build()
    );


    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private int timer;
    private int noBlockTimer;

    private BlockPos.Mutable pos1 = new BlockPos.Mutable(); // Rendering for cubes
    private BlockPos.Mutable pos2 = new BlockPos.Mutable();
    private Box box;
    int maxh = 0;
    int maxv = 0;


    public Nuker() {
        super(Categories.World, "nuker", "Breaks blocks around you.");
    }


    @Override
    public void onActivate() {
        firstBlock = true;
        timer = 0;
        noBlockTimer = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (enableRenderBounding.get()){
            // Render bounding box if cube and should break stuff
            if (shape.get() != Shape.Sphere && mode.get() != Mode.Smash) {
                box = new Box(pos1, pos2);
                event.renderer.box(box, sideColorBox.get(), lineColorBox.get(), shapeModeBox.get(), 0);
            }
        }

    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Update timer
        if (timer > 0) {
            timer--;
            return;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        if (shape.get() == Shape.UniformCube) range.set((double) Math.round(range.get()));

        // Some render stuff

        double pX_ = pX;
        double pZ_ = pZ;
        int r = (int) Math.round(range.get());

        if (shape.get() == Shape.UniformCube) {
            pX_ += 1; // weired position stuff
            pos1.set(pX_ - r, pY - r + 1, pZ - r+1); // down
            pos2.set(pX_ + r-1, pY + r, pZ + r); // up
        } else {
            int direction = Math.round((mc.player.getRotationClient().y % 360) / 90);
            direction = Math.floorMod(direction, 4);

            // direction == 1
            pos1.set(pX_ - (range_forward.get()), Math.ceil(pY) - range_down.get(), pZ_ - range_right.get()); // down
            pos2.set(pX_ + range_back.get()+1, Math.ceil(pY + range_up.get() + 1), pZ_ + range_left.get()+1); // up

            // Only change me if you want to mess with 3D rotations:
            if (direction == 2) {
                pX_ += 1;
                pZ_ += 1;
                pos1.set(pX_ - (range_left.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - (range_forward.get()+1)); // down
                pos2.set(pX_ + range_right.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_back.get()); // up
            } else if (direction == 3) {
                pX_ += 1;
                pos1.set(pX_ - (range_back.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - range_left.get()); // down
                pos2.set(pX_ + range_forward.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_right.get()+1); // up
            } else if (direction == 0) {
                pZ_ += 1;
                pX_ += 1;
                pos1.set(pX_ - (range_right.get()+1), Math.ceil(pY) - range_down.get(), pZ_ - (range_back.get()+1)); // down
                pos2.set(pX_ + range_left.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_forward.get()); // up
            }

            // get largest horizontal
            maxh = 1 + Math.max(Math.max(Math.max(range_back.get(),range_right.get()),range_forward.get()),range_left.get());
            maxv = 1 + Math.max(range_up.get(), range_down.get());
        }

        if (mode.get() == Mode.Flatten){
            pos1.setY((int) Math.floor(pY));
        }
        box = new Box(pos1, pos2);


        // Find blocks to break
        BlockIterator.register(Math.max((int) Math.ceil(range.get()+1), maxh), Math.max((int) Math.ceil(range.get()), maxv), (blockPos, blockState) -> {
            // Check for air, unbreakable blocks and distance
            boolean toofarSphere = Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq;
            boolean toofarUniformCube = maxDist(Math.floor(pX), Math.floor(pY), Math.floor(pZ), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get();
            boolean toofarCube = !box.contains(Vec3d.ofCenter(blockPos));

            if (!BlockUtils.canBreak(blockPos, blockState)
                || (toofarSphere && shape.get() == Shape.Sphere)
                || (toofarUniformCube && shape.get() == Shape.UniformCube)
                || (toofarCube && shape.get() == Shape.Cube))
                return;

            // Flatten
            if (mode.get() == Mode.Flatten && blockPos.getY() < Math.floor(mc.player.getY())) return;

            // Smash
            if (mode.get() == Mode.Smash && blockState.getHardness(mc.world, blockPos) != 0) return;

            // Check for selected
            if (whitelistEnabled.get() && !whitelist.get().contains(blockState.getBlock())) return;

            // Add block
            blocks.add(blockPosPool.get().set(blockPos));
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks

			if (sortMode.get() == SortMode.TopDown)
                blocks.sort(Comparator.comparingDouble(value -> -1*value.getY()));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // Check if some block was found
            if (blocks.isEmpty()) {
                // If no block was found for long enough then set firstBlock flag to true to not wait before breaking another again
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            }
            else {
                noBlockTimer = 0;
            }

            // Update timer
            if (!firstBlock && !lastBlockPos.equals(blocks.get(0))) {
                timer = delay.get();

                firstBlock = false;
                lastBlockPos.set(blocks.get(0));

                if (timer > 0) return;
            }

            // Break
            int count = 0;

            for (BlockPos block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(block);

                if (packetMine.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, block, Direction.UP));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, block, Direction.UP));
                } else {
                    BlockUtils.breakBlock(block, swingHand.get());
                }

                if (enableRenderBreaking.get()) RenderUtils.renderTickingBlock(block.toImmutable(), sideColor.get(), lineColor.get(), shapeModeBreak.get(), 0, 8, true, false);
                lastBlockPos.set(block);

                count++;
                if (!canInstaMine && !packetMine.get() /* With packet mine attempt to break everything possible at once */) break;
            }

            firstBlock = false;

            // Clear current block positions
            for (BlockPos.Mutable blockPos : blocks) blockPosPool.free(blockPos);
            blocks.clear();
        });
    }

    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        TopDown

    }
    public enum Shape {
        Cube,
        UniformCube,
        Sphere
    }


    public static double maxDist(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Gets the largest X, Y or Z difference, manhattan style
        double dX = Math.ceil(Math.abs(x2 - x1));
        double dY = Math.ceil(Math.abs(y2 - y1));
        double dZ = Math.ceil(Math.abs(z2 - z1));
        return Math.max(Math.max(dX, dY), dZ);
    }
}
