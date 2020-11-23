/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlaySoundEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class OffHandCrash extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doCrash = sgGeneral.add(new BoolSetting.Builder()
            .name("do-crash")
            .description("Sends x number of offhand swap sound packets to the server per tick.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
            .name("speed")
            .description("How many swaps per tick. 20 ticks = 1 second.")
            .defaultValue(2000)
            .min(1)
            .sliderMax(10000)
            .build()
    );

    private final Setting<Boolean> antiCrash = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-crash")
            .description("Prevents you from crashing as a result of offhand swapping.")
            .defaultValue(true)
            .build()
    );

    public OffHandCrash() {
        super(Category.Misc, "offhand-crash", "Swaps your hand and offhand items very fast which can crash other players around you.");
    }


    private static final PlayerActionC2SPacket PACKET = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0) , Direction.UP);

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (doCrash.get()) {
            for(int i = 0; i < speed.get(); ++i) mc.player.networkHandler.sendPacket(PACKET);
        }
    });

    @EventHandler
    private final Listener<PlaySoundEvent> onPlaySound = new Listener<>(event -> {
        if (antiCrash.get() && event.sound.getId().toString().equals("minecraft:item.armor.equip_generic")){
            event.cancel();
        }
    });
}