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
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class SelfWeb extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Places in your upper hitbox as well.")
            .defaultValue(false)
            .build()
    );

    public SelfWeb() {
        super(Category.Combat, "self-web", "Automatically places webs at your feet.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.COBWEB) {
                webSlot = i;
                break;
            }
        }
        if (webSlot == -1) return;

        int prevSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = webSlot;
        BlockPos playerPos = mc.player.getBlockPos();

        PlayerUtils.placeBlock(playerPos);
        if (doubles.get()) PlayerUtils.placeBlock(playerPos.add(0, 1, 0));

        mc.player.inventory.selectedSlot = prevSlot;
    });
}
