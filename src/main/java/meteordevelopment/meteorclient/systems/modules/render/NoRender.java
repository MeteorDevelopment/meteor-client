/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

import java.util.List;
import java.util.Set;

public class NoRender extends Module {
    private final SettingGroup sgOverlay = settings.createGroup("overlay");
    private final SettingGroup sgHUD = settings.createGroup("hud");
    private final SettingGroup sgWorld = settings.createGroup("world");
    private final SettingGroup sgEntity = settings.createGroup("entity");

    // Overlay

    private final Setting<Boolean> noPortalOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("portal-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSpyglassOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("spyglass-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noNausea = sgOverlay.add(new BoolSetting.Builder()
        .name("nausea")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPumpkinOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("pumpkin-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPowderedSnowOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("powdered-snow-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFireOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("fire-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noLiquidOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("liquid-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noInWallOverlay = sgOverlay.add(new BoolSetting.Builder()
        .name("in-wall-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noVignette = sgOverlay.add(new BoolSetting.Builder()
        .name("vignette")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noGuiBackground = sgOverlay.add(new BoolSetting.Builder()
        .name("gui-background")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noTotemAnimation = sgOverlay.add(new BoolSetting.Builder()
        .name("totem-animation")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEatParticles = sgOverlay.add(new BoolSetting.Builder()
        .name("eating-particles")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEnchantGlint = sgOverlay.add(new BoolSetting.Builder()
        .name("enchantment-glint")
        .defaultValue(false)
        .build()
    );

    // HUD

    private final Setting<Boolean> noBossBar = sgHUD.add(new BoolSetting.Builder()
        .name("boss-bar")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noScoreboard = sgHUD.add(new BoolSetting.Builder()
        .name("scoreboard")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noCrosshair = sgHUD.add(new BoolSetting.Builder()
        .name("crosshair")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> noTitle = sgHUD.add(new BoolSetting.Builder()
        .name("title")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noHeldItemName = sgHUD.add(new BoolSetting.Builder()
        .name("held-item-name")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noObfuscation = sgHUD.add(new BoolSetting.Builder()
        .name("obfuscation")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noPotionIcons = sgHUD.add(new BoolSetting.Builder()
        .name("potion-icons")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMessageSignatureIndicator = sgHUD.add(new BoolSetting.Builder()
        .name("message-signature-indicator")
        .defaultValue(false)
        .build()
    );

    // World

    private final Setting<Boolean> noWeather = sgWorld.add(new BoolSetting.Builder()
        .name("weather")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noWorldBorder = sgWorld.add(new BoolSetting.Builder()
        .name("world-border")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlindness = sgWorld.add(new BoolSetting.Builder()
        .name("blindness")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noDarkness = sgWorld.add(new BoolSetting.Builder()
        .name("darkness")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFog = sgWorld.add(new BoolSetting.Builder()
        .name("fog")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noEnchTableBook = sgWorld.add(new BoolSetting.Builder()
        .name("enchantment-table-book")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noSignText = sgWorld.add(new BoolSetting.Builder()
        .name("sign-text")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlockBreakParticles = sgWorld.add(new BoolSetting.Builder()
        .name("block-break-particles")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBlockBreakOverlay = sgWorld.add(new BoolSetting.Builder()
        .name("block-break-overlay")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noBeaconBeams = sgWorld.add(new BoolSetting.Builder()
        .name("beacon-beams")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noFallingBlocks = sgWorld.add(new BoolSetting.Builder()
        .name("falling-blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noCaveCulling = sgWorld.add(new BoolSetting.Builder()
        .name("cave-culling")
        .defaultValue(false)
        .onChanged(b -> mc.worldRenderer.reload())
        .build()
    );

    private final Setting<Boolean> noMapMarkers = sgWorld.add(new BoolSetting.Builder()
        .name("map-markers")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMapContents = sgWorld.add(new BoolSetting.Builder()
        .name("map-contents")
        .defaultValue(false)
        .build()
    );

    private final Setting<BannerRenderMode> bannerRender = sgWorld.add(new EnumSetting.Builder<BannerRenderMode>()
        .name("banners")
        .defaultValue(BannerRenderMode.Everything)
        .build()
    );

    private final Setting<Boolean> noFireworkExplosions = sgWorld.add(new BoolSetting.Builder()
        .name("firework-explosions")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<ParticleType<?>>> particles = sgWorld.add(new ParticleTypeListSetting.Builder()
        .name("particles")
        .build()
    );

    private final Setting<Boolean> noBarrierInvis = sgWorld.add(new BoolSetting.Builder()
        .name("barrier-invisibility")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noTextureRotations = sgWorld.add(new BoolSetting.Builder()
        .name("texture-rotations")
        .defaultValue(false)
        .onChanged(b -> mc.worldRenderer.reload())
        .build()
    );

    private final Setting<List<Block>> blockEntities = sgWorld.add(new BlockListSetting.Builder()
        .name("block-entities")
        .filter(block -> block instanceof BlockEntityProvider && !(block instanceof AbstractBannerBlock))
        .build()
    );

    // Entity

    private final Setting<Set<EntityType<?>>> entities = sgEntity.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .build()
    );

    private final Setting<Boolean> dropSpawnPacket = sgEntity.add(new BoolSetting.Builder()
        .name("drop-spawn-packets")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noArmor = sgEntity.add(new BoolSetting.Builder()
        .name("armor")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noInvisibility = sgEntity.add(new BoolSetting.Builder()
        .name("invisibility")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noGlowing = sgEntity.add(new BoolSetting.Builder()
        .name("glowing")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noMobInSpawner = sgEntity.add(new BoolSetting.Builder()
        .name("spawner-entities")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noDeadEntities = sgEntity.add(new BoolSetting.Builder()
        .name("dead-entities")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> noNametags = sgEntity.add(new BoolSetting.Builder()
        .name("nametags")
        .defaultValue(false)
        .build()
    );

    public NoRender() {
        super(Categories.Render, "no-render");
    }

    @Override
    public void onActivate() {
        if (noCaveCulling.get() || noTextureRotations.get()) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (noCaveCulling.get() || noTextureRotations.get()) mc.worldRenderer.reload();
    }

    // Overlay

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
    public boolean noTitle() {
        return isActive() && noTitle.get();
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

    public boolean noWorldBorder() {
        return isActive() && noWorldBorder.get();
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

    public boolean noMapContents() {
        return isActive() && noMapContents.get();
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

    public boolean noTextureRotations() {
        return isActive() && noTextureRotations.get();
    }

    @EventHandler
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        if (blockEntities.get().contains(event.blockEntityState.blockState.getBlock())) event.cancel();
    }

    // Entity

    public boolean noEntity(Entity entity) {
        return isActive() && entities.get().contains(entity.getType());
    }

    public boolean noEntity(EntityType<?> entity) {
        return isActive() && entities.get().contains(entity);
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
