/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SelfTrap extends Module {

    public SelfTrap(){
        super(Category.Combat, "self-trap", "Places obsidian around your head and upper body.");
    }

    public enum TopMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum BottomMode {
        Single,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
            .name("top-mode")
            .description("Which positions to place on your top half.")
            .defaultValue(TopMode.AntiFacePlace)
            .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
            .name("bottom-mode")
            .description("Which positions to place on your bottom half.")
            .defaultValue(BottomMode.None)
            .build()
    );

    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-off")
            .description("Toggles off after placing.")
            .defaultValue(true)
            .build()
    );

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int obsidianSlot = -1;
        for(int i = 0; i < 9; i++){
            if (mc.player.inventory.getStack(i).getItem() == Blocks.OBSIDIAN.asItem()){
                obsidianSlot = i;
                break;
            }
        }
        if (obsidianSlot == -1) return;

        int prevSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = obsidianSlot;
        BlockPos targetPosUp = mc.player.getBlockPos().up();
        BlockPos targetPos = mc.player.getBlockPos();

        //PLACEMENT
        switch(topPlacement.get()) {
            case Full:
                int blocksPlaced = 0;
                if(mc.world.getBlockState(targetPosUp.add(0, 1, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(0, 1, 0), false));
                    blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(1, 0, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(1, 0, 0), false));
                    blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(-1, 0, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(-1, 0, 0), false));
                    blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(0, 0, 1)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(0, 0, 1), false));
                    blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(0, 0, -1)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(0, 0, -1), false));
                    blocksPlaced++;
                }
                if (blocksPlaced >= 1) mc.player.swingHand(Hand.MAIN_HAND);
                break;
            case AntiFacePlace:
                int _blocksPlaced = 0;
                if(mc.world.getBlockState(targetPosUp.add(1, 0, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(1, 0, 0), false));
                    _blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(-1, 0, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(-1, 0, 0), false));
                    _blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(0, 0, 1)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(0, 0, 1), false));
                    _blocksPlaced++;
                }
                if(mc.world.getBlockState(targetPosUp.add(0, 0, -1)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, targetPosUp.add(0, 0, -1), false));
                    _blocksPlaced++;
                }
                if (_blocksPlaced >= 1) mc.player.swingHand(Hand.MAIN_HAND);
                break;
            case Top:
                if(mc.world.getBlockState(targetPosUp.add(0, 1, 0)).getMaterial().isReplaceable()){
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos().add(0, 1, 0), Direction.UP, targetPosUp.add(0, 1, 0), false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                break;
            case None:
        }

        switch(bottomPlacement.get()) {
            case Single:
                if (mc.world.getBlockState(targetPos.add(0, -1, 0)).isAir()) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, targetPos.add(0, -1, 0), true));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                break;
            case None:
        }
        mc.player.inventory.selectedSlot = prevSlot;
        if (toggleOff.get()) toggle();
    });
}
