/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to break when behind blocks.")
        .defaultValue(4.0)
        .min(0)
        .sliderMax(6)
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
        .build()
    );

    private final Setting<Nuker.SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<Nuker.SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(Nuker.SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Attempt to instamine everything at once.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> suitableTools = sgGeneral.add(new BoolSetting.Builder()
        .name("only-suitable-tools")
        .description("Only mines when using an appropriate for the block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> interact = sgGeneral.add(new BoolSetting.Builder()
        .name("interact")
        .description("Interacts with the block instead of mining.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side to the block being mined.")
        .defaultValue(true)
        .build()
    );

    // Whitelist and blacklist

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The blocks you don't want to mine.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<Keybind> selectBlockBind = sgWhitelist.add(new KeybindSetting.Builder()
        .name("select-block-bind")
        .description("Adds targeted block to list when this button is pressed.")
        .defaultValue(Keybind.none())
        .build()
    );

    // Rendering

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Whether to swing hand client-side.")
        .defaultValue(true)
        .build()
    );

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

    private final List<BlockPos> blocks = new ArrayList<>();
    private final Set<BlockPos> interacted = new ObjectOpenHashSet<>();

    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private int timer;
    private int noBlockTimer;

    private final BlockPos.Mutable pos1 = new BlockPos.Mutable(); // Rendering for cubes
    private final BlockPos.Mutable pos2 = new BlockPos.Mutable();
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
        interacted.clear();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (enableRenderBounding.get()) {
            // Render bounding box if cube and should break stuff
            if (shape.get() != Shape.Sphere && mode.get() != Mode.Smash) {
                int minX = Math.min(pos1.getX(), pos2.getX());
                int minY = Math.min(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());
                event.renderer.box(minX, minY, minZ, maxX, maxY, maxZ, sideColorBox.get(), lineColorBox.get(), shapeModeBox.get(), 0);
            }
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Press) addTargetedBlockToList();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Press) addTargetedBlockToList();
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Update timer
        if (timer > 0) {
            timer--;
            return;
        }

        // Calculate some stuff
        double pX = mc.player.getX(), pY = mc.player.getY(), pZ = mc.player.getZ();
        double rangeSq = Math.pow(range.get(), 2);
        BlockPos playerBlockPos = mc.player.getBlockPos();

        if (shape.get() == Shape.UniformCube) range.set((double) Math.round(range.get()));

        double pX_ = pX;
        double pZ_ = pZ;
        int r = (int) Math.round(range.get());

        if (shape.get() == Shape.UniformCube) {
            pX_ += 1; // weird position stuff
            pos1.set(pX_ - r, pY - r + 1, pZ - r + 1); // down
            pos2.set(pX_ + r - 1, pY + r, pZ + r); // up
            maxh = 0;
            maxv = 0;
        } else {
            // Only change me if you want to mess with 3D rotations:
            // I messed with it
            Direction direction = mc.player.getHorizontalFacing();
            switch (direction) {
                case Direction.SOUTH -> {
                    pZ_ += 1;
                    pX_ += 1;
                    pos1.set(pX_ - (range_right.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - (range_back.get() + 1)); // down
                    pos2.set(pX_ + range_left.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_forward.get()); // up
                }
                case Direction.WEST -> {
                    pos1.set(pX_ - range_forward.get(), Math.ceil(pY) - range_down.get(), pZ_ - range_right.get()); // down
                    pos2.set(pX_ + range_back.get() + 1, Math.ceil(pY + range_up.get() + 1), pZ_ + range_left.get() + 1); // up
                }
                case Direction.NORTH -> {
                    pX_ += 1;
                    pZ_ += 1;
                    pos1.set(pX_ - (range_left.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - (range_forward.get() + 1)); // down
                    pos2.set(pX_ + range_right.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_back.get()); // up
                }
                case Direction.EAST -> {
                    pX_ += 1;
                    pos1.set(pX_ - (range_back.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - range_left.get()); // down
                    pos2.set(pX_ + range_forward.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_right.get() + 1); // up
                }
            }

            // get largest horizontal
            maxh = 1 + Math.max(Math.max(Math.max(range_back.get(), range_right.get()), range_forward.get()), range_left.get());
            maxv = 1 + Math.max(range_up.get(), range_down.get());
        }

        // Flatten
        if (mode.get() == Mode.Flatten) pos1.setY((int) Math.floor(pY + 0.5));

        Box box = new Box(pos1.toCenterPos(), pos2.toCenterPos());

        // Find blocks to break
        BlockIterator.register(Math.max((int) Math.ceil(range.get() + 1), maxh), Math.max((int) Math.ceil(range.get()), maxv), (blockPos, blockState) -> {
            Vec3d center = blockPos.toCenterPos();
            switch (shape.get()) {
                case Sphere -> {
                    if (Utils.squaredDistance(pX, pY, pZ, center.getX(), center.getY(), center.getZ()) > rangeSq) return;
                }
                case UniformCube -> {
                    if (chebyshevDist(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get()) return;
                }
                case Cube -> {
                    if (!box.contains(center)) return;
                }
            }

            // Flatten
            if (mode.get() == Mode.Flatten && blockPos.getY() + 0.5 < pY) return;

            // Smash
            if (mode.get() == Mode.Smash && blockState.getHardness(mc.world, blockPos) != 0) return;

            // Use only optimal tools
            if (suitableTools.get() && !interact.get() && !mc.player.getMainHandStack().isSuitableFor(blockState)) return;

            // Block must be breakable
            if (!BlockUtils.canBreak(blockPos, blockState) && !interact.get()) return;

            // Raycast to block
            if (isOutOfRange(blockPos)) return;

            // Check whitelist or blacklist
            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(blockState.getBlock())) return;
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(blockState.getBlock())) return;

            if (interact.get() && interacted.contains(blockPos)) return;

            // Add block
            blocks.add(blockPos.toImmutable());
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() == SortMode.TopDown)
                blocks.sort(Comparator.comparingDouble(value -> -value.getY()));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // Check if some block was found
            if (blocks.isEmpty()) {
                interacted.clear();
                // If no block was found for long enough then set firstBlock flag to true to not wait before breaking another again
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            }
            else {
                noBlockTimer = 0;
            }

            // Update timer
            if (!firstBlock && !lastBlockPos.equals(blocks.getFirst())) {
                timer = delay.get();

                firstBlock = false;
                lastBlockPos.set(blocks.getFirst());

                if (timer > 0) return;
            }

            // Break
            int count = 0;

            for (BlockPos block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(block);

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(block), Rotations.getPitch(block), () -> breakBlock(block));
                else breakBlock(block);

                if (enableRenderBreaking.get()) RenderUtils.renderTickingBlock(block, sideColor.get(), lineColor.get(), shapeModeBreak.get(), 0, 8, true, false);
                lastBlockPos.set(block);

                count++;
                if (!canInstaMine && !packetMine.get() /* With packet mine attempt to break everything possible at once */) break;
            }

            firstBlock = false;

            // Clear current block positions
            blocks.clear();
        });
    }

    private void breakBlock(BlockPos blockPos) {
        if (interact.get()) {
            // Interact mode
            BlockUtils.interact(new BlockHitResult(blockPos.toCenterPos(), BlockUtils.getDirection(blockPos), blockPos, true), Hand.MAIN_HAND, swing.get());
            interacted.add(blockPos);
        } else if (packetMine.get()) {
            // Packet mine mode
            mc.interactionManager.sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos), sequence));

            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            mc.interactionManager.sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos), sequence));
        } else {
            // Legit mine mode
            BlockUtils.breakBlock(blockPos, swing.get());
        }
    }

    private boolean isOutOfRange(BlockPos blockPos) {
        Vec3d pos = blockPos.toCenterPos();
        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !PlayerUtils.isWithin(pos, wallsRange.get());

        return false;
    }

    private void addTargetedBlockToList() {
        if (!selectBlockBind.get().isPressed() || mc.currentScreen != null) return;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        Block targetBlock = mc.world.getBlockState(pos).getBlock();

        List<Block> list = listMode.get() == ListMode.Whitelist ? whitelist.get() : blacklist.get();
        String modeName = listMode.get().name();

        if (list.contains(targetBlock)) {
            list.remove(targetBlock);
            info("Removed " + Names.get(targetBlock) + " from " + modeName);
        } else {
            list.add(targetBlock);
            info("Added " + Names.get(targetBlock) + " to " + modeName);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = 0;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
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

    public static int chebyshevDist(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Gets the largest X, Y or Z difference, chebyshev distance
        int dX = Math.abs(x2 - x1);
        int dY = Math.abs(y2 - y1);
        int dZ = Math.abs(z2 - z1);
        return Math.max(Math.max(dX, dY), dZ);
    }
}
