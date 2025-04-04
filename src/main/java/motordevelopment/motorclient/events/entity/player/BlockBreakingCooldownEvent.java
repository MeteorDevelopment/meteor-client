/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity.player;

public class BlockBreakingCooldownEvent {
    private static final BlockBreakingCooldownEvent INSTANCE = new BlockBreakingCooldownEvent();

    public int cooldown;

    public static BlockBreakingCooldownEvent get(int cooldown) {
        INSTANCE.cooldown = cooldown;
        return INSTANCE;
    }
}
