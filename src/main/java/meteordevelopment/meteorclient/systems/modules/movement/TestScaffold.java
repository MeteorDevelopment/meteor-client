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
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
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
            FindItemResult item = InvUtils.find(stack -> stack.getItem() instanceof BlockItem);
            MeteorClient.INTERACTIONS.placeBlock(pos, item, InteractionManager.Priority.NORMAL);
            MeteorClient.INTERACTIONS.rotate(pos);
        }
    }
}
