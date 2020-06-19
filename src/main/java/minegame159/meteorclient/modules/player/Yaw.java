package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Yaw extends ToggleModule {
    private final SettingGroup sgAutoYaw = settings.createGroup("Auto Yaw", "auto", "Automatically uses the best angle.", true);

    private final Setting<Double> angle = sgAutoYaw.add(new DoubleSetting.Builder()
            .name("angle")
            .description("Angle in degrees.")
            .defaultValue(0)
            .build()
    );

    public Yaw() {
        super(Category.Player, "yaw", "Locks your yaw.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (sgAutoYaw.isEnabled()) mc.player.yaw = getYawDirection();
        else mc.player.yaw = angle.get().floatValue();
    });

    private float getYawDirection() {
        return Math.round((mc.player.yaw + 1f) / 45f) * 45f;
    }
}
