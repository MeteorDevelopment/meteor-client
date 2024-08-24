/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import com.google.common.collect.Streams;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scaffold extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Selected blocks.")
        .build()
    );

    private final Setting<ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("blocks-filter")
        .description("How to use the block list setting")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> fastTower = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-tower")
        .description("Whether or not to scaffold upwards faster.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> towerSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("tower-speed")
        .description("The speed at which to tower.")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> whileMoving = sgGeneral.add(new BoolSetting.Builder()
        .name("while-moving")
        .description("Allows you to tower while moving.")
        .defaultValue(false)
        .visible(fastTower::get)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only places blocks when holding right click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders your client-side swing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically swaps to a block before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allow air place. This also allows you to modify scaffold radius.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> aheadDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("ahead-distance")
        .description("How far ahead to place blocks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(1)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("closest-block-range")
        .description("How far can scaffold place blocks when you are in air.")
        .defaultValue(4)
        .min(0)
        .sliderMax(8)
        .visible(() -> !airPlace.get())
        .build()
    );

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Scaffold radius.")
        .defaultValue(0)
        .min(0)
        .max(6)
        .visible(airPlace::get)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(3)
        .min(1)
        .visible(airPlace::get)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render blocks that have been placed.")
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
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .visible(render::get)
        .build()
    );

    private final BlockPos.Mutable bp = new BlockPos.Mutable();

    public Scaffold() {
        super(Categories.Movement, "scaffold", "Automatically places blocks under you.");
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyOnClick.get() && !mc.options.useKey.isPressed()) return;

        Vec3d vec = mc.player.getPos().add(mc.player.getVelocity()).add(0, -0.75, 0);
        if (airPlace.get()) {
            bp.set(vec.getX(), vec.getY(), vec.getZ());
        } else {
            Vec3d pos = mc.player.getPos();
            if (aheadDistance.get() != 0 && !towering() && !mc.world.getBlockState(mc.player.getBlockPos().down()).getCollisionShape(mc.world, mc.player.getBlockPos()).isEmpty()) {
                Vec3d dir = Vec3d.fromPolar(0, mc.player.getYaw()).multiply(aheadDistance.get(), 0, aheadDistance.get());
                if (mc.options.forwardKey.isPressed()) pos = pos.add(dir.x, 0, dir.z);
                if (mc.options.backKey.isPressed()) pos = pos.add(-dir.x, 0, -dir.z);
                if (mc.options.leftKey.isPressed()) pos = pos.add(dir.z, 0, -dir.x);
                if (mc.options.rightKey.isPressed()) pos = pos.add(-dir.z, 0, dir.x);
            }
            bp.set(pos.x, vec.y, pos.z);
        }
        if (mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed() && mc.player.getY() + vec.y > -1) {
            bp.setY(bp.getY() - 1);
        }
        if (bp.getY() >= mc.player.getBlockPos().getY()) {
            bp.setY(mc.player.getBlockPos().getY() - 1);
        }
        BlockPos targetBlock = bp.toImmutable();

        if (!airPlace.get() && (BlockUtils.getPlaceSide(bp) == null)) {
            Vec3d pos = mc.player.getPos();
            pos = pos.add(0, -0.98f, 0);
            pos.add(mc.player.getVelocity());

            List<BlockPos> blockPosArray = new ArrayList<>();
            for (int x = (int) (mc.player.getX() - placeRange.get()); x < mc.player.getX() + placeRange.get(); x++) {
                for (int z = (int) (mc.player.getZ() - placeRange.get()); z < mc.player.getZ() + placeRange.get(); z++) {
                    for (int y = (int) Math.max(mc.world.getBottomY(), mc.player.getY() - placeRange.get()); y < Math.min(mc.world.getTopY(), mc.player.getY() + placeRange.get()); y++) {
                        bp.set(x, y, z);
                        if (BlockUtils.getPlaceSide(bp) == null) continue;
                        if (!BlockUtils.canPlace(bp)) continue;
                        if (mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(bp.offset(BlockUtils.getClosestPlaceSide(bp)))) > 36) continue;
                        blockPosArray.add(new BlockPos(bp));
                    }
                }
            }
            if (blockPosArray.isEmpty()) return;

            blockPosArray.sort(Comparator.comparingDouble((blockPos) -> blockPos.getSquaredDistance(targetBlock)));

            bp.set(blockPosArray.getFirst());
        }

        if (airPlace.get()) {
            List<BlockPos> blocks = new ArrayList<>();
            for (int x = (int) (bp.getX() - radius.get()); x <= bp.getX() + radius.get(); x++) {
                for (int z = (int) (bp.getZ() - radius.get()); z <= bp.getZ() + radius.get(); z++) {
                    BlockPos blockPos = BlockPos.ofFloored(x, bp.getY(), z);
                    if (mc.player.getPos().distanceTo(Vec3d.ofCenter(blockPos)) <= radius.get() || (x == bp.getX() && z == bp.getZ())) {
                        blocks.add(blockPos);
                    }
                }
            }

            if (!blocks.isEmpty()) {
                blocks.sort(Comparator.comparingDouble(PlayerUtils::squaredDistanceTo));
                int counter = 0;
                for (BlockPos block : blocks) {
                    if (place(block)) {
                        counter++;
                    }

                    if (counter >= blocksPerTick.get()) {
                        break;
                    }
                }
            }
        } else {
            place(bp);
        }

        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (fastTower.get() && mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed() && result.found() && (autoSwitch.get() || result.getHand() != null)) {
            Vec3d velocity = mc.player.getVelocity();
            Box playerBox = mc.player.getBoundingBox();
            if (Streams.stream(mc.world.getBlockCollisions(mc.player, playerBox.offset(0, 1, 0))).toList().isEmpty()) {
                // If there is no block above the player: move the player up, so he can place another block
                if (whileMoving.get() || !PlayerUtils.isMoving()) {
                    velocity = new Vec3d(velocity.x, towerSpeed.get(), velocity.z);
                }
                mc.player.setVelocity(velocity);
            } else {
                // If there is a block above the player: move the player down, so he's on top of the placed block
                mc.player.setVelocity(velocity.x, Math.ceil(mc.player.getY()) - mc.player.getY(), velocity.z);
                mc.player.setOnGround(true);
            }
        }
    }

    public boolean scaffolding() {
        return isActive() && (!onlyOnClick.get() || (onlyOnClick.get() && mc.options.useKey.isPressed()));
    }

    public boolean towering() {
        FindItemResult result = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        return scaffolding() && fastTower.get() && mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed() &&
            (whileMoving.get() || !PlayerUtils.isMoving()) && result.found() && (autoSwitch.get() || result.getHand() != null);
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    private boolean place(BlockPos bp) {
        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (!item.found()) return false;

        if (item.getHand() == null && !autoSwitch.get()) return false;

        if (BlockUtils.place(bp, item, rotate.get(), 50, renderSwing.get(), true)) {
            // Render block if was placed
            if (render.get())
                RenderUtils.renderTickingBlock(bp.toImmutable(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
            return true;
        }
        return false;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
