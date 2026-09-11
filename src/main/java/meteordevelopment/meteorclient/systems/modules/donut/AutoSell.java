/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between sell commands in ticks.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> sellAll = sgGeneral.add(new BoolSetting.Builder()
        .name("sell-all")
        .description("Use /sell all command.")
        .defaultValue(true)
        .build()
    );

    private int tickCounter = 0;

    public AutoSell() {
        super(Categories.Donut, "auto-sell", "Automatically sells items on Donut SMP.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= delay.get()) {
            tickCounter = 0;
            if (mc.player != null) {
                mc.player.connection.sendCommand(sellAll.get() ? "sell all" : "sell hand");
            }
        }
    }
}
