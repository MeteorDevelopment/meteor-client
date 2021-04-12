/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public class NoRender extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOverlay = settings.createGroup("Overlay");
    private final SettingGroup sgHUD = settings.createGroup("HUD");
    private final SettingGroup sgWorld = settings.createGroup("World");
    private final SettingGroup sgEntity = settings.createGroup("Entity");

    // Overlay

    private final Setting<Boolean> noHurtCam = sgOverlay.add(new BoolSetting.Builder()
            .name("no-hurt-cam")
            .description("Disables rendering of the hurt camera effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPortalOverlay = sgOverlay.add(new BoolSetting.Builder()
            .name("no-portal-overlay")
            .description("Disables rendering of the nether portal overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noNausea = sgOverlay.add(new BoolSetting.Builder()
            .name("no-nausea")
            .description("Disables rendering of the nausea overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPumpkinOverlay = sgOverlay.add(new BoolSetting.Builder()
            .name("no-pumpkin-overlay")
            .description("Disables rendering of the pumpkin head overlay")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFireOverlay = sgOverlay.add(new BoolSetting.Builder()
            .name("no-fire-overlay")
            .description("Disables rendering of the fire overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noWaterOverlay = sgOverlay.add(new BoolSetting.Builder()
            .name("no-water-overlay")
            .description("Disables rendering of the water overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noVignette = sgOverlay.add(new BoolSetting.Builder()
            .name("no-vignette")
            .description("Disables rendering of the vignette overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noGuiBackground = sgOverlay.add(new BoolSetting.Builder()
            .name("no-gui-background")
            .description("Disables rendering of the GUI background overlay.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noTotemAnimation = sgOverlay.add(new BoolSetting.Builder()
            .name("no-totem-animation")
            .description("Disables rendering of the totem animation when you pop a totem.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noEatParticles = sgOverlay.add(new BoolSetting.Builder()
            .name("no-eating-particles")
            .description("Disables rendering of eating particles.")
            .defaultValue(false)
            .build()
    );

    // HUD

    private final Setting<Boolean> noBossBar = sgHUD.add(new BoolSetting.Builder()
            .name("no-boss-bar")
            .description("Disable rendering of boss bars.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noScoreboard = sgHUD.add(new BoolSetting.Builder()
            .name("no-scoreboard")
            .description("Disable rendering of the scoreboard.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noCrosshair = sgHUD.add(new BoolSetting.Builder()
            .name("no-crosshair")
            .description("Disables rendering of the crosshair.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noPotionIcons = sgHUD.add(new BoolSetting.Builder()
            .name("no-potion-icons")
            .description("Disables rendering of status effect icons.")
            .defaultValue(false)
            .build()
    );

    // World

    private final Setting<Boolean> noWeather = sgWorld.add(new BoolSetting.Builder()
            .name("no-weather")
            .description("Disables rendering of weather.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFog = sgWorld.add(new BoolSetting.Builder()
            .name("no-fog")
            .description("Disables rendering of fog.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noEnchTableBook = sgWorld.add(new BoolSetting.Builder()
            .name("no-ench-table-book")
            .description("Disables rendering of books above enchanting tables.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noSignText = sgWorld.add(new BoolSetting.Builder()
            .name("no-sign-text")
            .description("Disables renedering of text on signs.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noBlockBreakParticles = sgWorld.add(new BoolSetting.Builder()
            .name("no-block-break-particles")
            .description("Disables rendering of block-break particles.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noSkylightUpdates = sgWorld.add(new BoolSetting.Builder()
            .name("no-skylight-updates")
            .description("Disables rendering of skylight updates.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noFallingBlocks = sgWorld.add(new BoolSetting.Builder()
            .name("no-falling-blocks")
            .description("Disables rendering of falling blocks.")
            .defaultValue(false)
            .build()
    );

    // Entity

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgEntity.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Disables rendering of selected entities.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .build()
    );


    private final Setting<Boolean> noArmor = sgEntity.add(new BoolSetting.Builder()
            .name("no-armor")
            .description("Disables rendering of armor on entities.")
            .defaultValue(false)
            .build()
    );

    public NoRender() {
        super(Categories.Render, "no-render", "Disables certain animations or overlays from rendering.");
    }

    // Overlay

    public boolean noHurtCam() {
        return isActive() && noHurtCam.get();
    }

    public boolean noPortalOverlay() {
        return isActive() && noPortalOverlay.get();
    }

    public boolean noNausea() {
        return isActive() && noNausea.get();
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

    public boolean noGuiBackground() {
        return isActive() && noGuiBackground.get();
    }

    public boolean noTotemAnimation() {
        return isActive() && noTotemAnimation.get();
    }

    public boolean noEatParticles() {
        return isActive() && noEatParticles.get();
    }

    // HUD

    public boolean noBossBar() {
        return isActive() && noBossBar.get();
    }

    public boolean noScoreboard() {
        return isActive() && noScoreboard.get();
    }

    public boolean noCrosshair() {
        return isActive() && noCrosshair.get();
    }

    public boolean noPotionIcons() {
        return isActive() && noPotionIcons.get();
    }

    // World

    public boolean noWeather() {
        return isActive() && noWeather.get();
    }

    public boolean noFog() {
        return isActive() && noFog.get();
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

    public boolean noSkylightUpdates() {
        return isActive() && noSkylightUpdates.get();
    }

    public boolean noFallingBlocks() {
        return isActive() && noFallingBlocks.get();
    }

    // Entity

    public boolean noEntity(Entity entity) {
        return isActive() && entities.get().getBoolean(entity.getType());
    }

    public boolean noArmor() {
        return isActive() && noArmor.get();
    }
}
