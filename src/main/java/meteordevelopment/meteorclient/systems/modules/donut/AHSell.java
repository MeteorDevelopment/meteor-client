package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;

public class AHSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> price = sgGeneral.add(new IntSetting.Builder()
        .name("price")
        .description("Price to list each hotbar item at.")
        .defaultValue(1000)
        .min(1)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait between listings, so the auction house keeps up.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private int slot;
    private int timer;

    public AHSell() {
        super(Categories.Donut, "ah-sell", "Lists every hotbar item on the auction house, then turns off.");
    }

    @Override
    public void onActivate() {
        slot = 0;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (timer > 0) {
            timer--;
            return;
        }

        while (slot < 9 && mc.player.getInventory().getItem(slot).isEmpty()) slot++;

        if (slot >= 9) {
            info("Listed all hotbar items.");
            toggle();
            return;
        }

        // /ah sell lists the held item, so select each slot before sending.
        InvUtils.swap(slot, false);
        mc.player.connection.sendCommand("ah sell " + price.get());

        slot++;
        timer = delay.get();
    }
}
