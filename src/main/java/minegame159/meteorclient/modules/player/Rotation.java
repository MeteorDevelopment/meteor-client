package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Rotation extends ToggleModule {
    // YAW
    private final SettingGroup sgYaw = settings.createGroup("Yaw", "lock-yaw", "Locks your yaw.", false);

    private final Setting<Double> yawAngle = sgYaw.add(new DoubleSetting.Builder()
            .name("yaw-angle")
            .description("Yaw angle in degrees.")
            .defaultValue(0)
            .build()
    );

    private final Setting<Boolean> yawAutoAngle = sgYaw.add(new BoolSetting.Builder()
            .name("yaw-auuto-angle")
            .description("Automatically uses the best angle.")
            .defaultValue(true)
            .build()
    );

    // PITCH
    private final SettingGroup sgPitch = settings.createGroup("Pitch", "lock-pitch", "Locks your pitch.", false);

    private final Setting<Double> pitchAngle = sgPitch.add(new DoubleSetting.Builder()
            .name("pitch-angle")
            .description("Pitch angle in degrees.")
            .defaultValue(0)
            .min(-90)
            .max(90)
            .build()
    );

    public Rotation() {
        super(Category.Player, "rotation", "Allows you to lock your yaw and pitch.");
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        // Yaw
        if (sgYaw.isEnabled()) {
            if (yawAutoAngle.get()) mc.player.yaw = getYawDirection();
            else mc.player.yaw = yawAngle.get().floatValue();
        }

        // Pitch
        if (sgPitch.isEnabled()) {
            mc.player.pitch = pitchAngle.get().floatValue();
        }
    });

    private float getYawDirection() {
        return Math.round((mc.player.yaw + 1f) / 45f) * 45f;
    }
}
