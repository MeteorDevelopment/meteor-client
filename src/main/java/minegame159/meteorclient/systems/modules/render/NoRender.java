/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class NoRender extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> noHurtCam = sgGeneral.add(new BoolSetting.Builder()
            .name("no-hurt-cam")
            .description("Disables the hurt camera effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noWeather = sgGeneral.add(new BoolSetting.Builder()
            .name("no-weather")
            .description("Disables the weather.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPortalOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-portal-overlay")
            .description("Disables all portal overlays.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noPumpkinOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-pumpkin-overlay")
            .description("Disables the pumpkin-head overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFireOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-fire-overlay")
            .description("Disables the fire overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noWaterOverlay = sgGeneral.add(new BoolSetting.Builder()
            .name("no-water-overlay")
            .description("Disables the water overlay.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noVignette = sgGeneral.add(new BoolSetting.Builder()
            .name("no-vignette")
            .description("Disables the vignette effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noBossBar = sgGeneral.add(new BoolSetting.Builder()
            .name("no-boss-bar")
            .description("Disables all boss bars from rendering.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noScoreboard = sgGeneral.add(new BoolSetting.Builder()
            .name("no-scoreboard")
            .description("Disable the scoreboard from rendering.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noFog = sgGeneral.add(new BoolSetting.Builder()
            .name("no-fog")
            .description("Disables all fog.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noTotemAnimation = sgGeneral.add(new BoolSetting.Builder()
            .name("no-totem-animation")
            .description("Disables the totem animation on your screen when popping a totem.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noArmor = sgGeneral.add(new BoolSetting.Builder()
            .name("no-armor")
            .description("Disables all armor from rendering.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noNausea = sgGeneral.add(new BoolSetting.Builder()
            .name("no-nausea")
            .description("Disables the nausea effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noItems = sgGeneral.add(new BoolSetting.Builder()
            .name("no-item")
            .description("Disables all item entities.")
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
            .description("Disables any text on signs. Useful for screenshots or streams.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noBlockBreakParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-block-break-particles")
            .description("Disables all block-break particles.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noFallingBlocks = sgGeneral.add(new BoolSetting.Builder()
            .name("no-falling-blocks")
            .description("Disables rendering of falling blocks. Useful for lag machines.")
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
            .description("Disables rendering of armor stands. Useful for lag machines.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noMinecarts = sgGeneral.add(new BoolSetting.Builder()
            .name("no-minecarts")
            .description("Disables rendering of minecarts.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noGuiBackground = sgGeneral.add(new BoolSetting.Builder()
            .name("no-gui-background")
            .description("Disables rendering of the dark GUI background.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noXpOrbs = sgGeneral.add(new BoolSetting.Builder()
            .name("no-xp-orbs")
            .description("Disables rendering of experience orb entities.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noEatParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-eating-particles")
            .description("Disables rendering of eating particles.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> noSkylightUpdates = sgGeneral.add(new BoolSetting.Builder()
            .name("no-skylight-updates")
            .description("Disables rendering of skylight updates. Useful for lag machines")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noCrosshair = sgGeneral.add(new BoolSetting.Builder()
            .name("no-crosshair")
            .description("Disables rendering of the crosshair.")
            .defaultValue(false)
            .build()
    );

    public NoRender() {
        super(Categories.Render, "no-render", "Disables certain animations or overlays from rendering.");
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

    public boolean noTotemAnimation() {
        return isActive() && noTotemAnimation.get();
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

    public boolean noMinecarts() {
        return isActive() && noMinecarts.get();
    }

    public boolean noGuiBackground() {
        return isActive() && noGuiBackground.get();
    }

    public boolean noXpOrbs() {
        return isActive() && noXpOrbs.get();
    }

    public boolean noEatParticles() {
        return isActive() && noEatParticles.get();
    }
    
    public boolean noSkylightUpdates() {
        return isActive() && noSkylightUpdates.get();
    }

    public boolean noCrosshair() {
        return isActive() && noCrosshair.get();
    }
}
