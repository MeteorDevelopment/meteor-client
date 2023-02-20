/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

import java.util.List;

public class NoRender extends Module {
    private final SettingGroup sgOverlay = settings.createGroup("Overlay");
    private final SettingGroup sgHUD = settings.createGroup("HUD");
    private final SettingGroup sgWorld = settings.createGroup("World");
    private final SettingGroup sgEntity = settings.createGroup("Entity");

    // Overlay

    private final Setting<Boolean> noHurtCam = sgOverlay.add(new BoolSetting.Builder()
        .name("hurt-cam")
        .description("Disables rendering of the hurt camera effect.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPortalOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("portal-overlay")
        .description("Disables rendering of the nether portal overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSpyglassOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("spyglass-overlay")
        .description("Disables rendering of the spyglass overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noNausea = sgOverlay.add(new BoolSetting.Builder()
        .name("nausea")
        .description("Disables rendering of the nausea overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPumpkinOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("pumpkin-overlay")
        .description("Disables rendering of the pumpkin head overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPowderedSnowOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("powdered-snow-overlay")
        .description("Disables rendering of the powdered snow overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFireOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("fire-overlay")
        .description("Disables rendering of the fire overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noLiquidOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("liquid-overlay")
        .description("Disables rendering of the liquid overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noInWallOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("in-wall-overlay")
        .description("Disables rendering of the overlay when inside blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noVignette = sgOverlay.add(new BoolSetting.Builder()
        .name("vignette")
        .description("Disables rendering of the vignette overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noGuiBackground = sgOverlay.add(new BoolSetting.Builder()
        .name("gui-background")
        .description("Disables rendering of the GUI background overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noTotemAnimation = sgOverlay.add(new BoolSetting.Builder()
        .name("totem-animation")
        .description("Disables rendering of the totem animation when you pop a totem.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEatParticles = sgOverlay.add(new BoolSetting.Builder()
        .name("eating-particles")
        .description("Disables rendering of eating particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEnchantGlint = sgOverlay.add(new BoolSetting.Builder()
        .name("enchantment-glint")
        .description("Disables rending of the enchantment glint.")
        .defaultValue(false)
        .build()
    );

    // HUD

    private final Setting<Boolean> noBossBar = sgHUD.add(new BoolSetting.Builder()
        .name("boss-bar")
        .description("Disable rendering of boss bars.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noScoreboard = sgHUD.add(new BoolSetting.Builder()
        .name("scoreboard")
        .description("Disable rendering of the scoreboard.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noCrosshair = sgHUD.add(new BoolSetting.Builder()
        .name("crosshair")
        .description("Disables rendering of the crosshair.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noHeldItemName = sgHUD.add(new BoolSetting.Builder()
        .name("held-item-name")
        .description("Disables rendering of the held item name.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noObfuscation = sgHUD.add(new BoolSetting.Builder()
        .name("obfuscation")
        .description("Disables obfuscation styling of characters.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPotionIcons = sgHUD.add(new BoolSetting.Builder()
        .name("potion-icons")
        .description("Disables rendering of status effect icons.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMessageSignatureIndicator = sgHUD.add(new BoolSetting.Builder()
        .name("message-signature-indicator")
        .description("Disables chat message signature indicator on the left of the message.")
        .defaultValue(false)
        .build()
    );

    // World

    private final Setting<Boolean> noWeather = sgWorld.add(new BoolSetting.Builder()
        .name("weather")
        .description("Disables rendering of weather.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlindness = sgWorld.add(new BoolSetting.Builder()
        .name("blindness")
        .description("Disables rendering of blindness.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noDarkness = sgWorld.add(new BoolSetting.Builder()
        .name("darkness")
        .description("Disables rendering of darkness.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFog = sgWorld.add(new BoolSetting.Builder()
        .name("fog")
        .description("Disables rendering of fog.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEnchTableBook = sgWorld.add(new BoolSetting.Builder()
        .name("enchantment-table-book")
        .description("Disables rendering of books above enchanting tables.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSignText = sgWorld.add(new BoolSetting.Builder()
        .name("sign-text")
        .description("Disables rendering of text on signs.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlockBreakParticles = sgWorld.add(new BoolSetting.Builder()
        .name("block-break-particles")
        .description("Disables rendering of block-break particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlockBreakOverlay = sgWorld.add(new BoolSetting.Builder()
        .name("block-break-overlay")
        .description("Disables rendering of block-break overlay.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSkylightUpdates = sgWorld.add(new BoolSetting.Builder()
        .name("skylight-updates")
        .description("Disables rendering of skylight updates.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBeaconBeams = sgWorld.add(new BoolSetting.Builder()
        .name("beacon-beams")
        .description("Disables rendering of beacon beams.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFallingBlocks = sgWorld.add(new BoolSetting.Builder()
        .name("falling-blocks")
        .description("Disables rendering of falling blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noCaveCulling = sgWorld.add(new BoolSetting.Builder()
        .name("cave-culling")
        .description("Disables Minecraft's cave culling algorithm.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMapMarkers = sgWorld.add(new BoolSetting.Builder()
        .name("map-markers")
        .description("Disables markers on maps.")
        .defaultValue(false)
        .build()
    );

    private final Setting<BannerRenderMode> bannerRender = sgWorld.add(new EnumSetting.Builder<BannerRenderMode>()
        .name("banners")
        .description("Changes rendering of banners.")
        .defaultValue(BannerRenderMode.Everything)
        .build()
    );

    private final Setting<Boolean> noFireworkExplosions = sgWorld.add(new BoolSetting.Builder()
        .name("firework-explosions")
        .description("Disables rendering of firework explosions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<ParticleType<?>>> particles = sgWorld.add(new ParticleTypeListSetting.Builder()
        .name("particles")
        .description("Particles to not render.")
        .build()
    );

    private final Setting<Boolean> noBarrierInvis = sgWorld.add(new BoolSetting.Builder()
        .name("barrier-invisibility")
        .description("Disables barriers being invisible when not holding one.")
        .defaultValue(false)
        .build()
    );

    // Entity

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgEntity.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Disables rendering of selected entities.")
        .build()
    );

    private final Setting<Boolean> dropSpawnPacket = sgEntity.add(new BoolSetting.Builder()
        .name("drop-spawn-packets")
        .description("WARNING! Drops all spawn packets of entities selected in the above list.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noArmor = sgEntity.add(new BoolSetting.Builder()
        .name("armor")
        .description("Disables rendering of armor on entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noInvisibility = sgEntity.add(new BoolSetting.Builder()
        .name("invisibility")
        .description("Shows invisible entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noGlowing = sgEntity.add(new BoolSetting.Builder()
        .name("glowing")
        .description("Disables rendering of the glowing effect")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMobInSpawner = sgEntity.add(new BoolSetting.Builder()
        .name("spawner-entities")
        .description("Disables rendering of spinning mobs inside of mob spawners")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noDeadEntities = sgEntity.add(new BoolSetting.Builder()
        .name("dead-entities")
        .description("Disables rendering of dead entities")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noNametags = sgEntity.add(new BoolSetting.Builder()
        .name("nametags")
        .description("Disables rendering of entity nametags")
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

    public boolean noSpyglassOverlay() {
        return isActive() && noSpyglassOverlay.get();
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

    public boolean noLiquidOverlay() {
        return isActive() && noLiquidOverlay.get();
    }

    public boolean noPowderedSnowOverlay() {
        return isActive() && noPowderedSnowOverlay.get();
    }

    public boolean noInWallOverlay() {
        return isActive() && noInWallOverlay.get();
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

    public boolean noEnchantGlint() {
        return isActive() && noEnchantGlint.get();
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

    public boolean noHeldItemName() {
        return isActive() && noHeldItemName.get();
    }

    public boolean noObfuscation() {
        return isActive() && noObfuscation.get();
    }

    public boolean noPotionIcons() {
        return isActive() && noPotionIcons.get();
    }

    public boolean noMessageSignatureIndicator() {
        return isActive() && noMessageSignatureIndicator.get();
    }

    // World

    public boolean noWeather() {
        return isActive() && noWeather.get();
    }

    public boolean noBlindness() {
        return isActive() && noBlindness.get();
    }

    public boolean noDarkness() {
        return isActive() && noDarkness.get();
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

    public boolean noBlockBreakOverlay() {
        return isActive() && noBlockBreakOverlay.get();
    }

    public boolean noSkylightUpdates() {
        return isActive() && noSkylightUpdates.get();
    }

    public boolean noBeaconBeams() {
        return isActive() && noBeaconBeams.get();
    }

    public boolean noFallingBlocks() {
        return isActive() && noFallingBlocks.get();
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        if (noCaveCulling.get()) event.cancel();
    }

    public boolean noMapMarkers() {
        return isActive() && noMapMarkers.get();
    }

    public BannerRenderMode getBannerRenderMode() {
        if (!isActive()) return BannerRenderMode.Everything;
        else return bannerRender.get();
    }

    public boolean noFireworkExplosions() {
        return isActive() && noFireworkExplosions.get();
    }

    @EventHandler
    private void onAddParticle(ParticleEvent event) {
        if (noWeather.get() && event.particle.getType() == ParticleTypes.RAIN) event.cancel();
        else if (noFireworkExplosions.get() && event.particle.getType() == ParticleTypes.FIREWORK) event.cancel();
        else if (particles.get().contains(event.particle.getType())) event.cancel();
    }

    public boolean noBarrierInvis() {
        return isActive() && noBarrierInvis.get();
    }

    // Entity

    public boolean noEntity(Entity entity) {
        return isActive() && entities.get().getBoolean(entity.getType());
    }

    public boolean noEntity(EntityType<?> entity) {
        return isActive() && entities.get().getBoolean(entity);
    }

    public boolean getDropSpawnPacket() {
        return isActive() && dropSpawnPacket.get();
    }

    public boolean noArmor() {
        return isActive() && noArmor.get();
    }

    public boolean noInvisibility() {
        return isActive() && noInvisibility.get();
    }

    public boolean noGlowing() {
        return isActive() && noGlowing.get();
    }

    public boolean noMobInSpawner() {
        return isActive() && noMobInSpawner.get();
    }

    public boolean noDeadEntities() {
        return isActive() && noDeadEntities.get();
    }

    public boolean noNametags() {
        return isActive() && noNametags.get();
    }

    public enum BannerRenderMode {
        Everything,
        Pillar,
        None
    }
}
