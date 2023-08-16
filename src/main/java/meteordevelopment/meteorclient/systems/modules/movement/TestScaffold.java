/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.interaction.api.InteractionManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class TestScaffold extends Module {
    public TestScaffold() {
        super(Categories.Movement, "test-scaffold", "Module to test the new interaction manager");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        BlockPos pos = mc.player.getBlockPos().down();
        BlockState blockState = mc.world.getBlockState(pos);

        if (blockState.isReplaceable()) {
            MeteorClient.INTERACTIONS.placeBlock(pos, null, InteractionManager.Priority.NORMAL);
        }
    }
}
