/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import java.util.*;

public class XrayBruteforce extends Module {
    public XrayBruteforce() {
        super(Categories.World, "Xray-BruteForce", "Bypass anti-xray.");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    private final Setting<Boolean> save_blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("save-scanned")
        .description("Save scanned blocks to memory.")
        .defaultValue(true)
        .onChanged(onChanged ->
        {
            scanned.clear();
        })
        .build()
    );

    private final Setting<Boolean> outline = sgVisual.add(new BoolSetting.Builder()
        .name("outline")
        .description("Outline to block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgVisual.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Outline color to block.")
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .visible(() -> outline.get())
        .build()
    );

    private final Setting<Boolean> tracer = sgVisual.add(new BoolSetting.Builder()
        .name("tracer")
        .description("Tracer to block.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgVisual.add(new ColorSetting.Builder()
        .name("tracer-color")
        .description("Tracer color to block.")
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .visible(() -> tracer.get())
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Bruteforce range.")
        .defaultValue(3)
        .min(3)
        .sliderRange(3, 80)
        .build()
    );
    private final Setting<Integer> y_range = sgGeneral.add(new IntSetting.Builder()
        .name("y-range")
        .description("Bruteforce range.")
        .defaultValue(13)
        .min(3)
        .sliderRange(0, 255)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Bruteforce delay.")
        .defaultValue(5)
        .min(3)
        .sliderRange(3, 20)
        .build()
    );
    private Thread newThread = null;

    BlockPos currentBlock;
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (currentBlock != null) {
            BlockPos bp = currentBlock;
            BlockState state = mc.world.getBlockState(bp);
            VoxelShape shape = state.getOutlineShape(mc.world, bp);

            if (shape.isEmpty()) return;
            if (outline.get()) {
                for (Box b : shape.getBoundingBoxes()) {
                    render(event, bp, b, outlineColor.get());
                }
            }
            if (tracer.get()) {
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, bp.getX(), bp.getY(), bp.getZ(), tracerColor.get());
            }
        }
    }
    private void render(Render3DEvent event, BlockPos bp, Box box, SettingColor color) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, new SettingColor(255, 255, 255, 255), color, ShapeMode.Lines, 0);
    }
    @Override
    public void onDeactivate() {
        Stop();
    }

    List<BlockPos> blocks = new ArrayList<BlockPos>();
    List<BlockPos> blocks_done = new ArrayList<BlockPos>();
    @Override
    public String getInfoString() {
        if (calculating) {
            return "calculating";
        }
        else if (optimize) {
            return "optimize";
        }
        else if (blocks_done.stream().count() == blocks.stream().count()) {
            return null;
        }
        else {
            return Long.toString(blocks_done.stream().count()) + " / " + Long.toString(blocks.stream().count());
        }
    }
    private void sleep(int delay)
    {
        try {
            Thread.sleep(delay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(BlockPos blockpos)
    {
        if (blockpos == null)
            return;
        ClientPlayNetworkHandler conn = mc.getNetworkHandler();
        if (conn == null)
            return;
        currentBlock = blockpos;
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, new BlockPos(blockpos), Direction.UP
        );
        blocks_done.add(blockpos);
        scanned.add(blockpos);
        conn.sendPacket(packet);
        sleep(delay.get());
    }

    private boolean wasPathing = false;
    private void Start() {
        newThread = new Thread(() -> {
            blocks = getBlocks(mc.player.getBlockPos(), range.get());
            ChatUtils.info("Xray BruteForce", "BruteForce stated: " + blocks.stream().count() + " blocks");
            for (BlockPos blockpos : blocks) {
                send(blockpos);
            }
            currentBlock = null;
            ChatUtils.info("Xray BruteForce", "BruteForce finished");
        });
        newThread.start();
    }
    @Override
    public void onActivate() {
        Start();
    }
    private void Stop()
    {
        blocks.clear();
        blocks_done.clear();
        if (newThread != null && newThread.isAlive()) {
            newThread.stop();
        }
        currentBlock = null;
    }
    private static List<BlockPos> scanned = new ArrayList<BlockPos>();

    private List<BlockPos> CalcClear(BlockPos pos)
    {
        List<BlockPos> temp = new ArrayList<BlockPos>();
        temp.add(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ()));
        temp.add(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ()));

        temp.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1));
        temp.add(new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1));

        temp.add(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ()));
        temp.add(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()));

        return temp;
    }

    private boolean calculating = false;
    private boolean optimize = false;
    private List<BlockPos> getBlocks(BlockPos startPos, int radius)
    {
        List<BlockPos> temp = new ArrayList<BlockPos>();
        calculating = true;
        for (int dy = -y_range.get(); dy <= y_range.get(); dy++) {
            if ((startPos.getY() + dy) < 0 || (startPos.getY() + dy) > 255) continue;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    BlockPos blockPos = new BlockPos(startPos.getX() + dx, startPos.getY() + dy, startPos.getZ() + dz);
                    if (EntityUtils.isInRenderDistance(blockPos)) {
                        BlockState state = mc.world.getBlockState(blockPos);
                        if (state.getMaterial() != Material.LAVA
                            && state.getMaterial() != Material.WATER
                            && state.getMaterial() != Material.AIR
                        ) {
                            if (save_blocks.get()) {
                                if (!scanned.contains(blockPos)) {
                                    temp.add(blockPos);
                                }
                            } else {
                                temp.add(blockPos);
                            }
                        }
                    }
                }
            }
        }
        calculating = false;
        optimize = true;
        Iterator<BlockPos> posts = temp.iterator();
        while (posts.hasNext())
        {
            BlockPos next = posts.next();
            var clear = CalcClear(next);
            for (BlockPos cl : clear)
            {
                if (next == cl) {
                    posts.remove();
                }
            }
        }
        optimize = false;
        return temp;
    }
}
