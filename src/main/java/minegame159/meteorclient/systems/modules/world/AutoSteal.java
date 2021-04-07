/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import java.util.concurrent.ThreadLocalRandom;

public class AutoSteal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelays = settings.createGroup("Delay");

    // General

    private final Setting<Boolean> stealButtonEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("steal-button-enabled")
            .description("Shows the Steal button on the container screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoStealEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-steal-enabled")
            .description("Starts the auto steal when a container open.")
            .defaultValue(false)
            .onChanged((bool_1) -> checkAutoSettings())
            .build()
    );

    private final Setting<Boolean> dumpButtonEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("dump-button-enabled")
            .description("Shows the Dump button on the container screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoDumpEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dump-enabled")
            .description("Start auto dump when a container opens.")
            .defaultValue(false)
            .onChanged((bool_1) -> checkAutoSettings())
            .build()
    );

    // Delay

    private final Setting<Integer> minimumDelay = sgDelays.add(new IntSetting.Builder()
            .name("min-delay")
            .description("The minimum delay between stealing the next stack in milliseconds.")
            .sliderMax(1000)
            .defaultValue(180)
            .build()
    );

    private final Setting<Integer> randomDelay = sgDelays.add(new IntSetting.Builder()
            .name("random-delay")
            .description("Randomly adds a delay of up to the specified time in milliseconds. Helps avoid anti-cheats.") // Actually ms - 1, due to the RNG excluding upper bound
            .min(0)
            .sliderMax(1000)
            .defaultValue(50)
            .build()
    );

    public AutoSteal() {
        super(Categories.World, "auto-steal", "Automatically dumps or steals from storage blocks.");
    }

    private void checkAutoSettings() {
        if (autoStealEnabled.get() && autoDumpEnabled.get()) {
            ChatUtils.error("You can't enable Auto Steal and Auto Dump at the same time!");
            autoDumpEnabled.set(false);
        }
    }

    private int getSleepTime() {
        return minimumDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, randomDelay.get()) : 0);
    }

    private int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    private void moveSlots(ScreenHandler handler, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack())
                continue;

            int sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen
            if (mc.currentScreen == null) break;

            InvUtils.quickMove().slotId(i);
        }
    }

    /**
     * Thread-blocking operation to steal from containers. You REALLY should use {@link #stealAsync(ScreenHandler)}
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    private void steal(ScreenHandler handler) {
        moveSlots(handler, 0, getRows(handler) * 9);
    }

    /**
     * Thread-blocking operation to dump to containers. You REALLY should use {@link #dumpAsync(ScreenHandler)}
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    private void dump(ScreenHandler handler) {
        int playerInvOffset = getRows(handler) * 9;
        moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9);
    }

    /**
     * Runs {@link #steal(ScreenHandler)} in a separate thread
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    public void stealAsync(ScreenHandler handler) {
        MeteorExecutor.execute(() -> steal(handler));
    }

    /**
     * Runs {@link #dump(ScreenHandler)} in a separate thread
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    public void dumpAsync(ScreenHandler handler) {
        MeteorExecutor.execute(() -> dump(handler));
    }

    public boolean getStealButtonEnabled() {
        return stealButtonEnabled.get();
    }

    public boolean getDumpButtonEnabled() {
        return dumpButtonEnabled.get();
    }

    public boolean getAutoStealEnabled() {
        return autoStealEnabled.get();
    }

    public boolean getAutoDumpEnabled() {
        return autoDumpEnabled.get();
    }

}
