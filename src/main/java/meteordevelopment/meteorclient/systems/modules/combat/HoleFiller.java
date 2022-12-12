/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class HoleFiller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSmart = settings.createGroup("Smart");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks can be used to fill holes.")
        .defaultValue(
            Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN,
            Blocks.NETHERITE_BLOCK,
            Blocks.RESPAWN_ANCHOR,
            Blocks.COBWEB
        )
        .build()
    );

    private final Setting<Integer> searchRadius = sgGeneral.add(new IntSetting.Builder()
        .name("search-radius")
        .description("Horizontal radius in which to search for holes.")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("How far away from the player you can place a block.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("doubles")
        .description("Fills double holes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the holes being filled.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The ticks delay between placement.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(3)
        .min(1)
        .build()
    );

    // Smart

    private final Setting<Boolean> smart = sgSmart.add(new BoolSetting.Builder()
        .name("smart")
        .description("Take more factors into account before filling a hole.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Keybind> forceFill = sgSmart.add(new KeybindSetting.Builder()
        .name("force-fill")
        .description("Fills all holes around you regardless of target checks.")
        .defaultValue(Keybind.none())
        .visible(smart::get)
        .build()
    );

    private final Setting<Boolean> predict = sgSmart.add(new BoolSetting.Builder()
        .name("predict")
        .description("Predict target movement to account for ping.")
        .defaultValue(true)
        .visible(smart::get)
        .build()
    );

    private final Setting<Boolean> ignoreSafe = sgSmart.add(new BoolSetting.Builder()
        .name("ignore-safe")
        .description("Ignore players in safe holes.")
        .defaultValue(true)
        .visible(smart::get)
        .build()
    );

    private final Setting<Boolean> onlyMoving = sgSmart.add(new BoolSetting.Builder()
        .name("only-moving")
        .description("Ignore players if they're standing still.")
        .defaultValue(true)
        .visible(smart::get)
        .build()
    );

    private final Setting<Double> targetRange = sgSmart.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("How far away to target players.")
        .defaultValue(7)
        .min(0)
        .sliderMin(1)
        .sliderMax(10)
        .visible(smart::get)
        .build()
    );

    private final Setting<Double> feetRange = sgSmart.add(new DoubleSetting.Builder()
        .name("feet-range")
        .description("How far from a hole a player's feet must be to fill it.")
        .defaultValue(1.5)
        .min(0)
        .sliderMax(4)
        .visible(smart::get)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swing the player's hand when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    private final Setting<SettingColor> nextSideColor = sgRender.add(new ColorSetting.Builder()
        .name("next-side-color")
        .description("The side color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245, 10))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> nextLineColor = sgRender.add(new ColorSetting.Builder()
        .name("next-line-color")
        .description("The line color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    private final List<PlayerEntity> targets = new ArrayList<>();
    private final List<Hole> holes = new ArrayList<>();
    private int timer;

    public HoleFiller() {
        super(Categories.Combat, "hole-filler", "Fills holes with specified blocks.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (smart.get()) setTargets();
        holes.clear();

        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) return;

        BlockIterator.register(searchRadius.get(), searchRadius.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int bedrock = 0, obsidian = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(blockPos.offset(direction));

                if (state.getBlock() == Blocks.BEDROCK) bedrock++;
                else if (state.getBlock() == Blocks.OBSIDIAN) obsidian++;
                else if (direction == Direction.DOWN) return;
                else if (validHole(blockPos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                        BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                        if (blockState1.getBlock() == Blocks.BEDROCK) bedrock++;
                        else if (blockState1.getBlock() == Blocks.OBSIDIAN) obsidian++;
                        else return;
                    }

                    air = direction;
                }

                if (obsidian + bedrock == 5 && air == null) holes.add(new Hole(blockPos, (byte) 0));
                else if (obsidian + bedrock == 8 && doubles.get() && air != null) {
                    holes.add(new Hole(blockPos, Dir.get(air)));
                }
            }
        });

        BlockIterator.after(() -> {
            if (timer <= 0 && !holes.isEmpty()) {
                int bpt = 0;

                for (Hole hole : holes) {
                    if (bpt >= blocksPerTick.get()) continue;
                    if (BlockUtils.place(hole.blockPos, block, rotate.get(), 10, swing.get(), true)) {
                        bpt++;
                    }
                }

                timer = placeDelay.get();
            }
        });

        timer--;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get() || holes.isEmpty()) return;

        for (Hole hole : holes) {
            boolean isNext = false;
            for (int i = 0; i < holes.size(); i++) {
                if (!holes.get(i).equals(hole)) continue;
                if (i < blocksPerTick.get()) isNext = true;
            }

            Color side = isNext ? nextSideColor.get() : sideColor.get();
            Color line = isNext ? nextLineColor.get() : lineColor.get();

            event.renderer.box(hole.blockPos, side, line, shapeMode.get(), hole.exclude);
        }
    }

    private boolean validHole(BlockPos pos) {
        if (mc.player.getBlockPos().equals(pos)) return false;
        if (distance(mc.player, pos, false) > placeRange.get()) return false;
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return false;

        if (smart.get() && !forceFill.get().isPressed()) {
            boolean validHole = false;
            for (PlayerEntity target : targets) {
                if (target.getY() > pos.getY()
                    && !target.getBlockPos().equals(pos)
                    && (distance(target, pos, true) < feetRange.get())
                ) validHole = true;
            }
            if (!validHole) return false;
        }

        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;
        return !((AbstractBlockAccessor) mc.world.getBlockState(pos.up()).getBlock()).isCollidable();
    }

    private void setTargets() {
        targets.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.distanceTo(mc.player) > targetRange.get()) continue;

            if (player.isCreative() || player == mc.player
                || player.isDead() || !Friends.get().shouldAttack(player)
                || (isSurrounded(player) && ignoreSafe.get())
            ) continue;

            if (onlyMoving.get()) {
                Vec3d velocity = new Vec3d(
                    player.getX() - player.prevX,
                    player.getY() - player.prevY,
                    player.getZ() - player.prevZ
                );
                if (velocity.length() == 0) continue;
            }

            if (ignoreSafe.get()) {
                if (isSurrounded(player)) continue;
            }

            targets.add(player);
        }
    }

    private boolean isSurrounded(PlayerEntity target) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;

            Block block = mc.world.getBlockState(target.getBlockPos().offset(dir)).getBlock();
            if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK && block != Blocks.RESPAWN_ANCHOR
                && block != Blocks.CRYING_OBSIDIAN && block != Blocks.NETHERITE_BLOCK) {
                return false;
            }
        }

        return true;
    }

    private double distance(PlayerEntity player, BlockPos pos, boolean feet) {
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (feet) center.add(0, 0.5, 0);

        Vec3d playerPos = player.getPos();
        if (!feet) playerPos.add(0, player.getEyeHeight(mc.player.getPose()), 0);
        else if (predict.get()) {
            playerPos.add(
                player.getX() - player.prevX,
                player.getY() - player.prevY,
                player.getZ() - player.prevZ
            );
        }

        double i = playerPos.x - center.x;
        double j = playerPos.y - center.y;
        double k = playerPos.z - center.z;

        return (Math.sqrt(i * i + j * j + k * k));
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;

        public Hole(BlockPos blockPos, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;
        }
    }
}
