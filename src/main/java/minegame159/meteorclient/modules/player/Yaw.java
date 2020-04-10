package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public class Yaw extends ToggleModule {
    private Setting<Boolean> auto;
    private Setting<Double> angle;

    public Yaw() {
        super(Category.Player, "yaw", "Locks your yaw.");

        auto = addSetting(new BoolSetting.Builder()
                .name("auto")
                .description("Automatically uses the best angle.")
                .defaultValue(true)
                .onChanged(aBoolean -> angle.setVisible(!aBoolean))
                .build()
        );

        angle = addSetting(new DoubleSetting.Builder()
                .name("angle")
                .description("Angle in degrees.")
                .defaultValue(0)
                .visible(false)
                .build()
        );
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (auto.get()) mc.player.yaw = getYawDirection();
        else mc.player.yaw = angle.get().floatValue();
    });

    private float getYawDirection() {
        return Math.round((mc.player.yaw + 1f) / 45f) * 45f;
    }
}
