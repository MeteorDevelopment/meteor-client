/*
 * Ember Client - UI Settings Module
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class EmberSettings extends Module {
    private final SettingGroup sgUI = settings.createGroup("UI");
    private final SettingGroup sgTopBar = settings.createGroup("Top Bar");

    // UI Settings
    public final Setting<SettingColor> accentColor = sgUI.add(new ColorSetting.Builder()
        .name("accent-color")
        .description("The accent color for the UI.")
        .defaultValue(new SettingColor(255, 100, 30, 255))
        .build()
    );

    public final Setting<SettingColor> backgroundColor = sgUI.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The background color for panels.")
        .defaultValue(new SettingColor(16, 16, 22, 252))
        .build()
    );

    public final Setting<Boolean> animations = sgUI.add(new BoolSetting.Builder()
        .name("animations")
        .description("Enable UI animations.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> animationSpeed = sgUI.add(new DoubleSetting.Builder()
        .name("animation-speed")
        .description("Speed of UI animations.")
        .defaultValue(1.0)
        .min(0.1)
        .max(3.0)
        .sliderRange(0.1, 3.0)
        .build()
    );

    // Top Bar Settings
    public final Setting<Boolean> topBarEnabled = sgTopBar.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Show the top bar HUD.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> showFps = sgTopBar.add(new BoolSetting.Builder()
        .name("show-fps")
        .description("Show FPS in the top bar.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> showPing = sgTopBar.add(new BoolSetting.Builder()
        .name("show-ping")
        .description("Show ping in the top bar.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> showTps = sgTopBar.add(new BoolSetting.Builder()
        .name("show-tps")
        .description("Show TPS in the top bar.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> showCoords = sgTopBar.add(new BoolSetting.Builder()
        .name("show-coords")
        .description("Show coordinates in the top bar.")
        .defaultValue(true)
        .build()
    );

    public EmberSettings() {
        super(Categories.Misc, "ember-settings", "Configure Ember Client UI and features.");
    }
}
