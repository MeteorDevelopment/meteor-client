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
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class SelfWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Places webs in your upper hitbox as well.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Toggles off after placing the webs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate downwards when placing webs.")
            .defaultValue(true)
            .build()
    );

    public SelfWeb() {
        super(Category.Combat, "self-web", "Automatically places webs on you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        int slot = findSlot();
        if (slot == -1) return;

        BlockPos blockPos = mc.player.getBlockPos();
        if (PlayerUtils.canPlace(blockPos)) {
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND));
            else PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND);
        }

        if (doubles.get()) {
            int slot2 = findSlot();
            if (slot2 == -1) return;

            BlockPos blockPos2 = mc.player.getBlockPos().add(0, 1, 0);
            if (PlayerUtils.canPlace(blockPos2)) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos2), Rotations.getPitch(blockPos2), () -> PlayerUtils.placeBlock(blockPos2, slot2, Hand.MAIN_HAND));
                else PlayerUtils.placeBlock(blockPos2, slot2, Hand.MAIN_HAND);
            }
        }

        if (turnOff.get()) toggle();
    }

    private int findSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.COBWEB) {
                return i;
            }
        }

        return -1;
    }
}
