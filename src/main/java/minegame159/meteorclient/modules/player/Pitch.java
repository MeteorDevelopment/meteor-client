package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public class Pitch extends ToggleModule {
    private Setting<Double> angle = addSetting(new DoubleSetting.Builder()
            .name("angle")
            .description("Angle in degrees.")
            .defaultValue(0)
            .min(-90)
            .max(90)
            .build()
    );

    public Pitch() {
        super(Category.Player, "pitch", "Locks your pitch.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        mc.player.pitch = angle.get().floatValue();
    });
}
