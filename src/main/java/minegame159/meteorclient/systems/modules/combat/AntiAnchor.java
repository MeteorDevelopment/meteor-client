/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.util.Hand;

public class AntiAnchor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Makes you rotate when placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing")
            .description("Swings your hand when placing.")
            .defaultValue(true)
            .build()
    );

    public AntiAnchor() {
        super(Categories.Combat, "anti-anchor", "Automatically prevents Anchor Aura by placing a slab on your head.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getBlockState(mc.player.getBlockPos().add(0, 2, 0)).getBlock() == Blocks.RESPAWN_ANCHOR
                && mc.world.getBlockState(mc.player.getBlockPos().add(0, 1, 0)).getBlock() == Blocks.AIR){

            boolean notSneaking = !mc.player.isSneaking();
            if (notSneaking) mc.player.setSneaking(true);

            BlockUtils.place(
                    mc.player.getBlockPos().add(0, 1, 0),
                    Hand.MAIN_HAND,
                    InvUtils.findItemInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof SlabBlock),
                    rotate.get(),
                    15,
                    swing.get(),
                    true,
                    true,
                    true
            );

            if (notSneaking) mc.player.setSneaking(false);
        }
    }
}
