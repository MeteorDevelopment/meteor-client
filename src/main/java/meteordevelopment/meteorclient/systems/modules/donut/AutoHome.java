/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;

public class AutoHome extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> homeName = sgGeneral.add(new StringSetting.Builder()
        .name("home-name")
        .description("Name of home to teleport to.")
        .defaultValue("home")
        .build()
    );

    private final Setting<Keybind> homeKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("home-key")
        .description("Key to teleport home.")
        .defaultValue(Keybind.none())
        .action(() -> goHome())
        .build()
    );

    private final Setting<Boolean> onLowHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("on-low-health")
        .description("Auto home when health is low.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> healthThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("health-threshold")
        .description("Health to trigger auto home.")
        .defaultValue(6)
        .min(1)
        .max(20)
        .sliderRange(1, 20)
        .visible(onLowHealth::get)
        .build()
    );

    public AutoHome() {
        super(Categories.Donut, "auto-home", "Quick home teleport for Donut SMP.");
    }

    private void goHome() {
        if (mc.player != null) {
            mc.player.connection.sendCommand("home " + homeName.get());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player != null && onLowHealth.get()) {
            if (mc.player.getHealth() <= healthThreshold.get()) {
                goHome();
                toggle();
            }
        }
    }
}
