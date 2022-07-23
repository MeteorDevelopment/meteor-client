/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.search;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.UnorderedArrayList;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.block.Block;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

public class SGroup {
    private static final Search search = Modules.get().get(Search.class);

    private final Block block;

    public final UnorderedArrayList<SBlock> blocks = new UnorderedArrayList<>();

    private double sumX, sumY, sumZ;

    public SGroup(Block block) {
        this.block = block;
    }

    public void add(SBlock block, boolean removeFromOld, boolean splitGroup) {
        blocks.add(block);
        sumX += block.x;
        sumY += block.y;
        sumZ += block.z;

        if (block.group != null && removeFromOld) block.group.remove(block, splitGroup);
        block.group = this;
    }

    public void add(SBlock block) {
        add(block, true, true);
    }

    public void remove(SBlock block, boolean splitGroup) {
        blocks.remove(block);
        sumX -= block.x;
        sumY -= block.y;
        sumZ -= block.z;

        if (blocks.isEmpty()) search.removeGroup(block.group);
        else if (splitGroup) {
            trySplit(block);
        }
    }

    public void remove(SBlock block) {
        remove(block, true);
    }

    private void trySplit(SBlock block) {
        Set<SBlock> neighbours = new ObjectOpenHashSet<>(6);

        for (int side : SBlock.SIDES) {
            if ((block.neighbours & side) == side) {
                SBlock neighbour = block.getSideBlock(side);
                if (neighbour != null) neighbours.add(neighbour);
            }
        }
        if (neighbours.size() <= 1) return;

        Set<SBlock> remainingBlocks = new ObjectOpenHashSet<>(blocks);
        Queue<SBlock> blocksToCheck = new ArrayDeque<>();

        blocksToCheck.offer(blocks.get(0));
        remainingBlocks.remove(blocks.get(0));
        neighbours.remove(blocks.get(0));

        loop: {
            while (!blocksToCheck.isEmpty()) {
                SBlock b = blocksToCheck.poll();

                for (int side : SBlock.SIDES) {
                    if ((b.neighbours & side) != side) continue;
                    SBlock neighbour = b.getSideBlock(side);

                    if (neighbour != null && remainingBlocks.contains(neighbour)) {
                        blocksToCheck.offer(neighbour);
                        remainingBlocks.remove(neighbour);

                        neighbours.remove(neighbour);
                        if (neighbours.isEmpty()) break loop;
                    }
                }
            }
        }

        if (neighbours.size() > 0) {
            SGroup group = search.newGroup(this.block);
            group.blocks.ensureCapacity(remainingBlocks.size());

            blocks.removeIf(remainingBlocks::contains);

            for (SBlock b : remainingBlocks) {
                group.add(b, false, false);

                sumX -= b.x;
                sumY -= b.y;
                sumZ -= b.z;
            }

            if (neighbours.size() > 1) {
                block.neighbours = 0;

                for (SBlock b : neighbours) {
                    int x = b.x - block.x;
                    if (x == 1) block.neighbours |= SBlock.RI;
                    else if (x == -1) block.neighbours |= SBlock.LE;

                    int y = b.y - block.y;
                    if (y == 1) block.neighbours |= SBlock.TO;
                    else if (y == -1) block.neighbours |= SBlock.BO;

                    int z = b.z - block.z;
                    if (z == 1) block.neighbours |= SBlock.FO;
                    else if (z == -1) block.neighbours |= SBlock.BA;
                }

                group.trySplit(block);
            }
        }
    }

    public void merge(SGroup group) {
        blocks.ensureCapacity(blocks.size() + group.blocks.size());
        for (SBlock block : group.blocks) add(block, false, false);
        search.removeGroup(group);
    }

    public void render(Render3DEvent event) {
        SBlockData blockData = search.getBlockData(block);

        if (blockData.tracer) {
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, sumX / blocks.size() + 0.5, sumY / blocks.size() + 0.5, sumZ / blocks.size() + 0.5, blockData.tracerColor);
        }
    }
}
