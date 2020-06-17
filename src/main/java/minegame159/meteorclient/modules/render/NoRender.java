package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class NoRender extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noBubbles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-bubbles")
            .description("Disables rendering of bubbles in water.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noHurtCam = sgGeneral.add(new BoolSetting.Builder()
            .name("no-hurt-cam")
            .description("Disables hurt camera effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noWeather = sgGeneral.add(new BoolSetting.Builder()
            .name("no-weather")
            .description("Disables weather.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPortalOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-portal-overlay")
            .description("Disables portal overflay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPumpkinOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-pumpkin-overlay")
            .description("Disables pumpkin overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFireOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-fire-overlay")
            .description("Disables fire overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noWaterOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-water-overlay")
            .description("Disables water overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noVignette = sgGeneral.add(new BoolSetting.Builder()
            .name("no-vignette")
            .description("Disables vignette effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noBossBar = sgGeneral.add(new BoolSetting.Builder()
            .name("no-boss-bar")
            .description("Disables boss bars.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noScoreboard = sgGeneral.add(new BoolSetting.Builder()
            .name("no-scoreboard")
            .description("Disable scoreboard.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFog = sgGeneral.add(new BoolSetting.Builder()
            .name("no-fog")
            .description("Disables fog.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noExplosion = sgGeneral.add(new BoolSetting.Builder()
            .name("no-explosion")
            .description("Disables explosion particles.")
            .defaultValue(true)
            .build()
    );

    public NoRender() {
        super(Category.Render, "no-render", "Disables some things from rendering.");
    }

    public boolean noBubbles() {
        return isActive() && noBubbles.get();
    }

    public boolean noHurtCam() {
        return isActive() && noHurtCam.get();
    }

    public boolean noWeather() {
        return isActive() && noWeather.get();
    }

    public boolean noPortalOverlay() {
        return isActive() && noPortalOverlay.get();
    }

    public boolean noPumpkinOverlay() {
        return isActive() && noPumpkinOverlay.get();
    }

    public boolean noFireOverlay() {
        return isActive() && noFireOverlay.get();
    }

    public boolean noWaterOverlay() {
        return isActive() && noWaterOverlay.get();
    }

    public boolean noVignette() {
        return isActive() && noVignette.get();
    }

    public boolean noBossBar() {
        return isActive() && noBossBar.get();
    }

    public boolean noScoreboard() {
        return isActive() && noScoreboard.get();
    }

    public boolean noFog() {
        return isActive() && noFog.get();
    }

    public boolean noExplosion() {
        return isActive() && noExplosion.get();
    }
}
