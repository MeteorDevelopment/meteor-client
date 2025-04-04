/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.combat;

import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.player.InvUtils;
import motordevelopment.motorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class AntiAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings your hand client-side when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Makes you rotate when placing.")
        .defaultValue(true)
        .build()
    );

    public AntiAnvil() {
        super(Categories.Combat, "anti-anvil", "Automatically prevents Auto Anvil by placing between you and the anvil.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (int i = 0; i <= mc.player.getBlockInteractionRange(); i++) {
            BlockPos pos = mc.player.getBlockPos().add(0, i + 3, 0);

            if (mc.world.getBlockState(pos).getBlock() == Blocks.ANVIL && mc.world.getBlockState(pos.down()).isAir()) {
                if (BlockUtils.place(pos.down(), InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 15, swing.get(), true))
                    break;
            }
        }
    }
}
