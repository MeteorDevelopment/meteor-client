/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class SelfTrap extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Toggles when one placed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles when you run out of obsidian.")
            .defaultValue(false)
            .build()
    );

    public SelfTrap(){
        super(Category.Combat, "self-trap", "Places obsidian above your head.");
    }

    private boolean sentMessage = false;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int obsidianSlot = -1;
        for(int i = 0; i < 9; i++){
            if (mc.player.inventory.getStack(i).getItem() == Blocks.OBSIDIAN.asItem()){
                obsidianSlot = i;
                break;
            }
        }

        if (obsidianSlot == -1 && selfToggle.get()) {
            if (!sentMessage) {
                Chat.warning(this, "No obsidian found… disabling.");
                sentMessage = true;
            }

            this.toggle();
            return;
        } else if (obsidianSlot == -1) return;

        int prevSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = obsidianSlot;
        BlockPos targetPos = mc.player.getBlockPos().up(2);

        PlayerUtils.placeBlock(targetPos);

        if (turnOff.get()) toggle();
        mc.player.inventory.selectedSlot = prevSlot;
    });
}
