/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.MobSpawnerLogicAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.UnorderedArrayList;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ESPGroup {
    private static final BlockESP blockEsp = Modules.get().get(BlockESP.class);

    private final Block block;

    public final UnorderedArrayList<ESPBlock> blocks = new UnorderedArrayList<>();

    private double sumX, sumY, sumZ;

    public ESPGroup(Block block) {
        this.block = block;
    }

    public void add(ESPBlock block, boolean removeFromOld, boolean splitGroup) {
        blocks.add(block);
        sumX += block.x;
        sumY += block.y;
        sumZ += block.z;

        if (block.group != null && removeFromOld) block.group.remove(block, splitGroup);
        block.group = this;
    }

    public void add(ESPBlock block) {
        add(block, true, true);
    }

    public void remove(ESPBlock block, boolean splitGroup) {
        blocks.remove(block);
        sumX -= block.x;
        sumY -= block.y;
        sumZ -= block.z;

        if (blocks.isEmpty()) blockEsp.removeGroup(block.group);
        else if (splitGroup) {
            trySplit(block);
        }
    }

    public void remove(ESPBlock block) {
        remove(block, true);
    }

    private void trySplit(ESPBlock block) {
        Set<ESPBlock> neighbours = new ObjectOpenHashSet<>(6);

        for (int side : ESPBlock.SIDES) {
            if ((block.neighbours & side) == side) {
                ESPBlock neighbour = block.getSideBlock(side);
                if (neighbour != null) neighbours.add(neighbour);
            }
        }
        if (neighbours.size() <= 1) return;

        Set<ESPBlock> remainingBlocks = new ObjectOpenHashSet<>(blocks);
        Queue<ESPBlock> blocksToCheck = new ArrayDeque<>();

        blocksToCheck.offer(blocks.getFirst());
        remainingBlocks.remove(blocks.getFirst());
        neighbours.remove(blocks.getFirst());

        loop: {
            while (!blocksToCheck.isEmpty()) {
                ESPBlock b = blocksToCheck.poll();

                for (int side : ESPBlock.SIDES) {
                    if ((b.neighbours & side) != side) continue;
                    ESPBlock neighbour = b.getSideBlock(side);

                    if (neighbour != null && remainingBlocks.contains(neighbour)) {
                        blocksToCheck.offer(neighbour);
                        remainingBlocks.remove(neighbour);

                        neighbours.remove(neighbour);
                        if (neighbours.isEmpty()) break loop;
                    }
                }
            }
        }

        if (!neighbours.isEmpty()) {
            ESPGroup group = blockEsp.newGroup(this.block);
            group.blocks.ensureCapacity(remainingBlocks.size());

            blocks.removeIf(remainingBlocks::contains);

            for (ESPBlock b : remainingBlocks) {
                group.add(b, false, false);

                sumX -= b.x;
                sumY -= b.y;
                sumZ -= b.z;
            }

            if (neighbours.size() > 1) {
                block.neighbours = 0;

                for (ESPBlock b : neighbours) {
                    int x = b.x - block.x;
                    if (x == 1) block.neighbours |= ESPBlock.RI;
                    else if (x == -1) block.neighbours |= ESPBlock.LE;

                    int y = b.y - block.y;
                    if (y == 1) block.neighbours |= ESPBlock.TO;
                    else if (y == -1) block.neighbours |= ESPBlock.BO;

                    int z = b.z - block.z;
                    if (z == 1) block.neighbours |= ESPBlock.FO;
                    else if (z == -1) block.neighbours |= ESPBlock.BA;
                }

                group.trySplit(block);
            }
        }
    }

    public void merge(ESPGroup group) {
        blocks.ensureCapacity(blocks.size() + group.blocks.size());
        for (ESPBlock block : group.blocks) add(block, false, false);
        blockEsp.removeGroup(group);
    }

    public void render(Render3DEvent event) {
        ESPBlockData blockData = blockEsp.getBlockData(block);

        if (blockData.tracer) {
            int x = (int) (sumX / blocks.size());
            int y = (int) (sumY / blocks.size());
            int z = (int) (sumZ / blocks.size());
            if (mc.world != null && block instanceof SpawnerBlock && blockEsp.isOnlyActivatedSpawners()) {
                BlockEntity blockEntity = mc.world.getBlockEntity(new BlockPos(x, y, z));
                if (blockEntity instanceof MobSpawnerBlockEntity spawner) {
                    MobSpawnerLogicAccessor logic = (MobSpawnerLogicAccessor) spawner.getLogic();
                    if (logic.meteor$getSpawnDelay() != 20) {
                        event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, sumX / blocks.size() + 0.5, sumY / blocks.size() + 0.5, sumZ / blocks.size() + 0.5, blockData.tracerColor);
                    }
                }
            } else {
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, sumX / blocks.size() + 0.5, sumY / blocks.size() + 0.5, sumZ / blocks.size() + 0.5, blockData.tracerColor);
            }
        }
    }
}
