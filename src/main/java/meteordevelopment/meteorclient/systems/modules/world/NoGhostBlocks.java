/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;

public class NoGhostBlocks extends Module {

    public NoGhostBlocks() {
        super(Categories.World, "no-ghost-blocks", "Attempts to prevent ghost blocks arising from breaking blocks quickly. Especially useful with multiconnect.");
    }

    private BlockState lastState;

    @EventHandler
    public void onBreakBlock(BreakBlockEvent event) {
        if (mc.isInSingleplayer())
            return;

        event.setCancelled(true);

        // play the related sounds and particles for the user.
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        blockState.getBlock().onBreak(mc.world, event.blockPos, blockState, mc.player); // this doesn't alter the state of the block in the world
    }
}
