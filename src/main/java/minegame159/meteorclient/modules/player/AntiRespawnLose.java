/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Created By Danik#9249 27/12/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;


public class AntiRespawnLose extends ToggleModule {


    public AntiRespawnLose() {
        super(Category.Player, "anti-respawn-lose", "Prevents you from lose you respawn point.");
    }

    @EventHandler
    private final Listener<SendPacketEvent> StopPacket = new Listener<>(event -> {
        if (mc.world == null) return;
        if(!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;
        

        BlockPos blockPos = ((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getBlockPos();
        boolean IsOverWorld = mc.world.getDimension().isBedWorking();
        boolean IsNetherWorld = mc.world.getDimension().isRespawnAnchorWorking();
        boolean BlockIsBed = mc.world.getBlockState(blockPos).getBlock() instanceof BedBlock;
        boolean BlockIsAnchor = mc.world.getBlockState(blockPos).getBlock().equals(Blocks.RESPAWN_ANCHOR);

        if((BlockIsBed && IsOverWorld)||(BlockIsAnchor && IsNetherWorld)) {
            mc.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(mc.world, blockPos));
            mc.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(mc.world, blockPos.offset(((PlayerInteractBlockC2SPacket) event.packet).getBlockHitResult().getSide())));
            event.cancel();
        }

    });

}
