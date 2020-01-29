package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiWeather extends Module {
    public AntiWeather() {
        super(Category.Misc, "anti-weather", "Disables weather.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.world.isRaining()) mc.world.setRainGradient(0);
    });
}
