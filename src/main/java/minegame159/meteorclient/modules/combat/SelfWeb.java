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
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
        super(Category.Combat, "Self-Web", "Automatically places webs on you.");
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

        if (rotate.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(mc.player.yaw, 90, mc.player.isOnGround()));
        }
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, playerPos, true));

        if (doubles.get()) mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, playerPos.add(0 ,1,0), true));

        mc.player.inventory.selectedSlot = prevSlot;
        if (turnOff.get()) toggle();
    });
}
