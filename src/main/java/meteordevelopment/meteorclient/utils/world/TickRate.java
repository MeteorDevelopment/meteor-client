/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;

/**
 * Copied from <a href="https://github.com/S-B99/kamiblue/blob/feature/master/src/main/java/me/zeroeightsix/kami/util/LagCompensator.java">KAMI Blue</a>
 */
public class TickRate {
    public static TickRate INSTANCE = new TickRate();

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    private TickRate() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof WorldTimeUpdateS2CPacket) {
            long now = System.currentTimeMillis();
            float timeElapsed = (now - timeLastTimeUpdate) / 1000.0F;
            tickRates[nextIndex] = MathHelper.clamp(20.0f / timeElapsed, 0.0f, 20.0f);
            nextIndex = (nextIndex + 1) % tickRates.length;
            timeLastTimeUpdate = now;
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        Arrays.fill(tickRates, 0);
        nextIndex = 0;
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    public float getTickRate() {
        if (!Utils.canUpdate()) return 0;
        if (System.currentTimeMillis() - timeGameJoined < 4000) return 20;

        int numTicks = 0;
        float sumTickRates = 0.0f;
        for (float tickRate : tickRates) {
            if (tickRate > 0) {
                sumTickRates += tickRate;
                numTicks++;
            }
        }
        return sumTickRates / numTicks;
    }

    public float getTimeSinceLastTick() {
        long now = System.currentTimeMillis();
        if (now - timeGameJoined < 4000) return 0;
        return (now - timeLastTimeUpdate) / 1000f;
    }
}
