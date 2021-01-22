package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.misc.ThreadUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public class AutoSteal extends Module {
    public AutoSteal() {
        super(Category.Player, "auto-steal", "Buttons for automatically dumps/steals from chests.");   // TODO: grammar
    }

    // TODO: grammar (descriptions)
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> stealButtonEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("steal-button-enabled")
            .description("Shows Steal button on container screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> dumpButtonEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("dump-button-enabled")
            .description("Shows Dump button on container screen.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoStealEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-steal-enabled")
            .description("Start steals when container open.")
            .defaultValue(false)
            .onChanged((bool_1) -> checkAutoSettings())
            .build()
    );

    private final Setting<Boolean> autoDumpEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dump-enabled")
            .description("Start dumps when container open.")
            .defaultValue(false)
            .onChanged((bool_1) -> checkAutoSettings())
            .build()
    );


    private final SettingGroup sgDelays = settings.createGroup("Delays");

    private final Setting<Integer> minimumDelay = sgDelays.add(new IntSetting.Builder()
            .name("min-delay")
            .description("Minimum delay between stealing the next stack in milliseconds. Use 0 to steal the entire inventory instantly.")
            .min(0)
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

    private void checkAutoSettings() {
        if (autoStealEnabled.get() && autoDumpEnabled.get()) {
            ChatUtils.error("Can't enabled auto-steal and auto-dump at the same time!");
            autoDumpEnabled.set(false);
        }
    }

    private int getSleepTime() {
        return minimumDelay.get() + (randomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, randomDelay.get()) : 0);
    }

    private void moveSlots(GenericContainerScreenHandler handler, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack())
                continue;

            int sleep = getSleepTime();
            if (sleep > 0)
                ThreadUtils.sleep(sleep);

            // Exit if user closes screen
            if (mc.currentScreen == null)
                break;

            InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
        }
    }

    /**
     * Thread-blocking operation to steal from containers. You REALLY should use {@link #stealAsync(GenericContainerScreenHandler)}
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    private void steal(GenericContainerScreenHandler handler) {
        moveSlots(handler, 0, handler.getRows() * 9);
    }

    /**
     * Thread-blocking operation to dump to containers. You REALLY should use {@link #dumpAsync(GenericContainerScreenHandler)}
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    private void dump(GenericContainerScreenHandler handler) {
        int playerInvOffset = handler.getRows() * 9;
        moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9);
    }

    /**
     * Runs {@link #steal(GenericContainerScreenHandler)} in a separate thread
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    public void stealAsync(GenericContainerScreenHandler handler) {
        ThreadUtils.runInThread(() -> steal(handler));
    }

    /**
     * Runs {@link #dump(GenericContainerScreenHandler)} in a separate thread
     *
     * @param handler Passed in from {@link minegame159.meteorclient.mixin.GenericContainerScreenMixin}
     */
    public void dumpAsync(GenericContainerScreenHandler handler) {
        ThreadUtils.runInThread(() -> dump(handler));
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
