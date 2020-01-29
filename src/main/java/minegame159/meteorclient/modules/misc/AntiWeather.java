package minegame159.meteorclient.modules.misc;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiWeather extends Module {
    public AntiWeather() {
        super(Category.Misc, "anti-weather", "Disables weather.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mc.world.isRaining()) mc.world.setRainGradient(0);
    }
}
