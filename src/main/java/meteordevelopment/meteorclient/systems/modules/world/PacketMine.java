/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class PacketMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks in ticks.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switches to the best tool when the block is ready to be mined instantly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> notOnUse = sgGeneral.add(new BoolSetting.Builder()
        .name("not-on-use")
        .description("Won't auto switch if you're using an item.")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Boolean> obscureBreakingProgress = sgGeneral.add(new BoolSetting.Builder()
        .name("obscure-breaking-progress")
        .description("Spams abort breaking packets to obscure the block mining progress from other players. Does not hide it perfectly.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> readySideColor = sgRender.add(new ColorSetting.Builder()
        .name("ready-side-color")
        .description("The color of the sides of the blocks that can be broken.")
        .defaultValue(new SettingColor(0, 204, 0, 10))
        .build()
    );

    private final Setting<SettingColor> readyLineColor = sgRender.add(new ColorSetting.Builder()
        .name("ready-line-color")
        .description("The color of the lines of the blocks that can be broken.")
        .defaultValue(new SettingColor(0, 204, 0, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    public final List<MyBlock> blocks = new ArrayList<>();

    private boolean swapped, shouldUpdateSlot;

    public PacketMine() {
        super(Categories.World, "packet-mine", "Sends packets to mine blocks without the mining animation.");
    }

    @Override
    public void onActivate() {
        swapped = false;
    }

    @Override
    public void onDeactivate() {
        blockPool.freeAll(blocks);
        blocks.clear();

        if (shouldUpdateSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            shouldUpdateSlot = false;
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!BlockUtils.canBreak(event.blockPos)) return;
        event.cancel();

        swapped = false;

        if (!isMiningBlock(event.blockPos)) {
            blocks.add(blockPool.get().set(event));
        }
    }

    public boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocks.removeIf(MyBlock::shouldRemove);

        if (shouldUpdateSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            shouldUpdateSlot = false;
            swapped = false;
        }

        if (!blocks.isEmpty()) {
            MyBlock block = blocks.getFirst();
            block.mine();

            if (block.isReady() && !swapped && autoSwitch.get() && (!mc.player.isUsingItem() || !notOnUse.get())) {
                FindItemResult slot = InvUtils.findFastestTool(block.blockState);
                if (!slot.found() || mc.player.getInventory().getSelectedSlot() == slot.slot()) return;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot.slot()));
                swapped = true;
                shouldUpdateSlot = true;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (MyBlock block : blocks) {
            if (!Modules.get().get(BreakIndicators.class).isActive() || !Modules.get().get(BreakIndicators.class).packetMine.get() || !block.mining) {
                block.render(event);
            }
        }
    }

    public class MyBlock {
        public BlockPos blockPos;
        public BlockState blockState;
        public Block block;

        public Direction direction;

        public int timer, startTime;
        public boolean mining;

        public MyBlock set(StartBreakingBlockEvent event) {
            this.blockPos = event.blockPos;
            this.direction = event.direction;
            this.blockState = mc.world.getBlockState(blockPos);
            this.block = blockState.getBlock();
            this.timer = delay.get();
            this.mining = false;

            return this;
        }

        public boolean shouldRemove() {
            boolean broken = mc.world.getBlockState(blockPos).getBlock() != block;
            boolean timeout = progress() > 2 && (mc.player.age - startTime > 50);
            boolean distance = Utils.distance(mc.player.getEyePos().x, mc.player.getEyePos().y, mc.player.getEyePos().z, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.player.getBlockInteractionRange();

            return broken || timeout || distance;
        }

        public boolean isReady() {
            return progress() >= 1;
        }

        public double progress() {
            if (!mining) return 0;

            FindItemResult fir = InvUtils.findFastestTool(blockState);
            return BlockUtils.getBreakDelta(fir.found() ? fir.slot() : mc.player.getInventory().getSelectedSlot(), blockState) * ((mc.player.age - startTime) + 1);
        }

        public void mine() {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::sendMinePackets);
            else sendMinePackets();
        }

        private void sendMinePackets() {
            if (timer <= 0) {
                if (!mining) {
                    mc.interactionManager.sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
                    mc.interactionManager.sendSequencedPacket(mc.world, (sequence) -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence));

                    mining = true;
                    startTime = mc.player.age;
                }
            }
            else {
                timer--;
            }

            if (mining && obscureBreakingProgress.get()) mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
        }

        public void render(Render3DEvent event) {
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            if (!shape.isEmpty()) {
                x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
                y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
                z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
                x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
                y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
                z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
            }

            if (isReady()) {
                event.renderer.box(x1, y1, z1, x2, y2, z2, readySideColor.get(), readyLineColor.get(), shapeMode.get(), 0);
            } else {
                event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }
}
