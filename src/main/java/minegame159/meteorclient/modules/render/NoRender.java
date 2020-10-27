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
            .description("Disables portal overlay.")
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

    private final Setting<Boolean> noTotem = sgGeneral.add(new BoolSetting.Builder()
            .name("no-totem")
            .description("Disables totem particles.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noArmor = sgGeneral.add(new BoolSetting.Builder()
            .name("no-armor")
            .description("Disables player armor.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noNausea = sgGeneral.add(new BoolSetting.Builder()
            .name("no-nausea")
            .description("Disables nausea effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noItems = sgGeneral.add(new BoolSetting.Builder()
            .name("no-item")
            .description("Disables item entities.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noEnchTableBook = sgGeneral.add(new BoolSetting.Builder()
            .name("no-ench-table-book")
            .description("Disables book above enchanting table.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noSignText = sgGeneral.add(new BoolSetting.Builder()
            .name("no-sign-text")
            .description("Disables text on signs.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noBlockBreakParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-block-break-particles")
            .description("Disables block break particles.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noFallingBlocks = sgGeneral.add(new BoolSetting.Builder()
            .name("no-falling-blocks")
            .description("Disables rendering of falling blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noPotionIcons = sgGeneral.add(new BoolSetting.Builder()
            .name("no-potion-icons")
            .description("Disables rendering of status effect icons.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noArmorStands = sgGeneral.add(new BoolSetting.Builder()
            .name("no-armor-stands")
            .description("Disables rendering of armor stands.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noGuiBackground = sgGeneral.add(new BoolSetting.Builder()
            .name("no-gui-background")
            .description("Disables rendering of the gui background.")
            .defaultValue(false)
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

    public boolean noTotem() {
        return isActive() && noTotem.get();
    }

    public boolean noArmor() {
        return isActive() && noArmor.get();
    }

    public boolean noNausea() {
        return isActive() && noNausea.get();
    }

    public boolean noItems() {
        return isActive() && noItems.get();
    }

    public boolean noEnchTableBook() {
        return isActive() && noEnchTableBook.get();
    }

    public boolean noSignText() {
        return isActive() && noSignText.get();
    }

    public boolean noBlockBreakParticles() {
        return isActive() && noBlockBreakParticles.get();
    }

    public boolean noFallingBlocks() {
        return isActive() && noFallingBlocks.get();
    }

    public boolean noPotionIcons() {
        return isActive() && noPotionIcons.get();
    }

    public boolean noArmorStands() {
        return isActive() && noArmorStands.get();
    }

    public boolean noGuiBackground() {
        return isActive() && noGuiBackground.get();
    }
}
