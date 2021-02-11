/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */
package minegame159.meteorclient.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class AntiAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate upwards when placing obsidian above you.")
            .defaultValue(true)
            .build()
    );

    public AntiAnvil(){
        super(Category.Combat, "anti-anvil", "Automatically prevents Auto Anvil by placing obsidian above you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (int i = 2; i <= mc.interactionManager.getReachDistance() + 2; i++){
            if (mc.world.getBlockState(mc.player.getBlockPos().add(0, i, 0)).getBlock() == Blocks.ANVIL && mc.world.getBlockState(mc.player.getBlockPos().add(0, i - 1, 0)).isAir()){
                int slot = InvUtils.findItemWithCount(Items.OBSIDIAN).slot;
                boolean stop = false;

                if (slot != 1 && slot < 9) {
                    firstThing(i, slot);
                    stop = true;
                }
                else if (mc.player.getOffHandStack().getItem() == Items.OBSIDIAN){
                    firstThing(i, -1);
                    stop = true;
                }

                if (stop) break;
            }
        }
    }

    private void firstThing(int i, int slot) {
        if (rotate.get()) Rotations.rotate(mc.player.yaw, -90, 15, () -> doThing(i, slot));
        else doThing(i, slot);
    }

    private void doThing(int i, int slot) {
        if (slot != -1) PlayerUtils.placeBlock(mc.player.getBlockPos().add(0, i - 2, 0), slot, InvUtils.getHand(Items.OBSIDIAN));
        else PlayerUtils.placeBlock(mc.player.getBlockPos().add(0, i - 2, 0),  InvUtils.getHand(Items.OBSIDIAN));
    }
}
