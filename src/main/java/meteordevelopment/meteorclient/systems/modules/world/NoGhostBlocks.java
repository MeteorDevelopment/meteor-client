/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

public class NoGhostBlocks extends Module {
    public NoGhostBlocks() {
        super(Categories.World, "no-ghost-blocks", "Attempts to prevent ghost blocks arising from breaking blocks quickly. Especially useful with multiconnect.");
    }

    @EventHandler
    public void onBreakBlock(BreakBlockEvent event) {
        if (mc.isInSingleplayer()) return;

        event.cancel();

        BlockState blockState = mc.world.getBlockState(event.blockPos);
        blockState.getBlock().onBreak(mc.world, event.blockPos, blockState, mc.player);
    }
    @EventHandler
    public void onInteractBlock(InteractBlockEvent event) {
        if (mc.isInSingleplayer()) return;
        if (event.result.getType() == HitResult.Type.MISS) return;
        
        ItemStack itemStack = event.hand == Hand.OFF_HAND
            ? mc.player.getOffHandStack()
            : mc.player.getMainHandStack();

        if (itemStack.getItem() instanceof BlockItem) {
            mc.player.swingHand(event.hand);
            //Send place packet
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(event.hand, event.result, 42069));

            event.cancel();
        }




    }
}
