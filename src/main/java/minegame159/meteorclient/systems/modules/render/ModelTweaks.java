/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.RenderEntityEvent;
import minegame159.meteorclient.mixininterface.IEntityRenderer;
import minegame159.meteorclient.mixininterface.ILivingEntityRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.text.TextUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;

import static net.minecraft.client.render.entity.LivingEntityRenderer.getOverlay;

public class ModelTweaks extends Module {

    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");

    //Players
    public final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables model tweaks for players.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> ignoreSelf = sgPlayers.add(new BoolSetting.Builder()
            .name("ignore-self")
            .description("Ignores yourself when tweaking player models.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> playersScale = sgPlayers.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Players scale.")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    private final Setting<Boolean> playerTexture = sgPlayers.add(new BoolSetting.Builder()
            .name("texture")
            .description("Enables player model textures.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> playersColor = sgPlayers.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of player models.")
            .defaultValue(new SettingColor(255, 255, 255, 100, 0.007))
            .build()
    );

    public final Setting<Boolean> useNameColor = sgPlayers.add(new BoolSetting.Builder()
            .name("use-name-color")
            .description("Uses players name color for the color.")
            .defaultValue(false)
            .build()
    );

    //Crystals
    public final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
            .name("enabled")
            .description("Enables model tweaks for end crystals.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> crystalsScale = sgCrystals.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Crystal scale.")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    public final Setting<Double> crystalsBounce = sgCrystals.add(new DoubleSetting.Builder()
            .name("bounce")
            .description("Crystal bounce.")
            .defaultValue(1.0)
            .min(0.0)
            .build()
    );

    public final Setting<Double> crystalsRotationAngle = sgCrystals.add(new DoubleSetting.Builder()
            .name("rotation-angle")
            .description("Crystal model part rotation angle.")
            .defaultValue(60)
            .min(0)
            .sliderMax(360)
            .max(360)
            .build()
    );

    public final Setting<Double> crystalsRotationSpeed = sgCrystals.add(new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("Crystal model part rotation speed.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> crystalsTexture = sgCrystals.add(new BoolSetting.Builder()
            .name("texture")
            .description("Enables crystal model textures.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> renderBottom = sgCrystals.add(new BoolSetting.Builder()
            .name("render-bottom")
            .description("Enables rendering of the bottom part of the crystal.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> crystalsBottomColor = sgCrystals.add(new ColorSetting.Builder()
            .name("bottom-color")
            .description("The color of end crystal models.")
            .defaultValue(new SettingColor(255, 255, 255, 0.007))
            .build()
    );

    public final Setting<Boolean> renderCore = sgCrystals.add(new BoolSetting.Builder()
            .name("render-core")
            .description("Enables rendering of the core of the crystal.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> crystalsCoreColor = sgCrystals.add(new ColorSetting.Builder()
            .name("core-color")
            .description("The color of end crystal models.")
            .defaultValue(new SettingColor(255, 255, 255, 0.007))
            .build()
    );

    public final Setting<Boolean> renderFrame = sgCrystals.add(new BoolSetting.Builder()
            .name("render-frame")
            .description("Enables rendering of the frame of the crystal.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> crystalsFrameColor = sgCrystals.add(new ColorSetting.Builder()
            .name("frame-color")
            .description("The color of end crystal models.")
            .defaultValue(new SettingColor(255, 255, 255, 0.007))
            .build()
    );

    private final float SINE_45_DEGREES = (float) Math.sin(0.7853981633974483D);
    private final Identifier WHITE = new Identifier("meteor-client", "entity-texture.png");
    private final Identifier TEXTURE = new Identifier("textures/entity/end_crystal/end_crystal.png");

    public ModelTweaks() {
        super(Categories.Render, "model-tweaks", "Changes the way certain entity models are rendered.");
    }

    //LivingEntity
    @EventHandler
    private void onRenderLiving(RenderEntityEvent.LiveEntity event) {
        if (event.entity instanceof PlayerEntity && players.get() && (!ignoreSelf.get() || event.entity != mc.player)) event.setCancelled(true);
        else return;

        ILivingEntityRenderer renderer = ((ILivingEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(event.entity));
        Color color = useNameColor.get() ? TextUtils.getMostPopularColor(event.entity.getDisplayName()) : playersColor.get();

        event.matrixStack.push();
        event.matrixStack.scale(playersScale.get().floatValue(), playersScale.get().floatValue(), playersScale.get().floatValue());

        event.model.handSwingProgress = event.entity.getHandSwingProgress(event.tickDelta);
        event.model.riding = event.entity.hasVehicle();
        event.model.child = event.entity.isBaby();

        float h = MathHelper.lerpAngleDegrees(event.tickDelta, event.entity.prevBodyYaw, event.entity.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(event.tickDelta, event.entity.prevHeadYaw, event.entity.headYaw);
        float k = j - h;
        float o;

        if (event.entity.hasVehicle() && event.entity.getVehicle() instanceof LivingEntity) {
            LivingEntity livingEntity2 = (LivingEntity) event.entity.getVehicle();

            h = MathHelper.lerpAngleDegrees(event.tickDelta, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
            k = j - h;
            o = MathHelper.wrapDegrees(k);

            if (o < -85.0F) o = -85.0F;
            if (o >= 85.0F) o = 85.0F;

            h = j - o;

            if (o * o > 2500.0F) h += o * 0.2F;

            k = j - h;
        }

        float m = MathHelper.lerp(event.tickDelta, event.entity.prevPitch, event.entity.pitch);
        float p;
        if (event.entity.getPose() == EntityPose.SLEEPING) {
            Direction direction = event.entity.getSleepingDirection();
            if (direction != null) {
                p = event.entity.getEyeHeight(EntityPose.STANDING) - 0.1F;
                event.matrixStack.translate((float)(-direction.getOffsetX()) * p, 0.0D, (float)(-direction.getOffsetZ()) * p);
            }
        }

        o = (float) event.entity.age + event.tickDelta;
        renderer.setupTransformsInterface(event.entity, event.matrixStack, o, h, event.tickDelta);

        event.matrixStack.scale(-1.0F, -1.0F, 1.0F);
        renderer.scaleInterface(event.entity, event.matrixStack, event.tickDelta);
        event.matrixStack.translate(0.0D, -1.5010000467300415D, 0.0D);
        p = 0.0F;
        float q = 0.0F;
        if (!event.entity.hasVehicle() && event.entity.isAlive()) {
            p = MathHelper.lerp(event.tickDelta, event.entity.lastLimbDistance, event.entity.limbDistance);
            q = event.entity.limbAngle - event.entity.limbDistance * (1.0F - event.tickDelta);
            if (event.entity.isBaby()) {
                q *= 3.0F;
            }

            if (p > 1.0F) {
                p = 1.0F;
            }
        }

        event.model.animateModel(event.entity, q, p, event.tickDelta);
        event.model.setAngles(event.entity, q, p, o, k, m);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        boolean bl = renderer.isVisibleInterface(event.entity);
        boolean bl2 = !bl && !event.entity.isInvisibleTo(minecraftClient.player);
        boolean bl3 = minecraftClient.hasOutline(event.entity);

        RenderLayer renderLayer = getRenderLayer(event.entity, bl, bl2, bl3, event);

        if (renderLayer != null) {
            VertexConsumer vertexConsumer = event.vertexConsumerProvider.getBuffer(renderLayer);
            int r = getOverlay(event.entity, renderer.getAnimationCounterInterface(event.entity, event.tickDelta));
            event.model.render(event.matrixStack, vertexConsumer, event.light, r, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        }

        if (!event.entity.isSpectator()) {
            for (FeatureRenderer<LivingEntity, EntityModel<LivingEntity>> livingEntityEntityModelFeatureRenderer : event.features) {
                livingEntityEntityModelFeatureRenderer.render(event.matrixStack, event.vertexConsumerProvider, event.light, event.entity, q, p, event.tickDelta, o, k, m);
            }
        }

        event.matrixStack.pop();
    }

    protected RenderLayer getRenderLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline, RenderEntityEvent.LiveEntity event) {
        IEntityRenderer renderer = ((IEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(event.entity));

        Identifier identifier = playerTexture.get() ? renderer.getTextureInterface(entity) : WHITE;

        if (translucent) return RenderLayer.getItemEntityTranslucentCull(identifier);
        else if (showBody) return event.model.getLayer(identifier);
        else return showOutline ? RenderLayer.getOutline(identifier) : null;
    }

    //Crystals
    @EventHandler
    private void onRenderCrystal(RenderEntityEvent.Crystal event) {
        if (crystals.get()) event.setCancelled(true);
        else return;

        Color bottomColor = crystalsBottomColor.get();
        Color coreColor = crystalsCoreColor.get();
        Color frameColor = crystalsFrameColor.get();

        RenderLayer END_CRYSTAL = crystalsTexture.get() ? RenderLayer.getEntityCutoutNoCull(TEXTURE) : RenderLayer.getEntityCutoutNoCull(WHITE);

        event.matrixStack.push();
        event.matrixStack.scale(crystalsScale.get().floatValue(), crystalsScale.get().floatValue(), crystalsScale.get().floatValue());

        float j = ((float) event.endCrystalEntity.endCrystalAge + event.tickDelta) * (3.0F * crystalsRotationSpeed.get().floatValue());
        float h = getYOffset(event.endCrystalEntity, event.tickDelta);
        int renderLayer = OverlayTexture.DEFAULT_UV;
        VertexConsumer vertexConsumer = event.vertexConsumerProvider.getBuffer(END_CRYSTAL);

        event.matrixStack.push();
        event.matrixStack.scale(2.0F, 2.0F, 2.0F);
        event.matrixStack.translate(0.0D, -0.5D, 0.0D);

        if (event.endCrystalEntity.getShowBottom() && renderBottom.get()) {
            event.bottom.render(event.matrixStack, vertexConsumer, event.light, renderLayer, bottomColor.r / 255f, bottomColor.g / 255f, bottomColor.b / 255f, bottomColor.a / 255f);
        }

        if (renderFrame.get()) {
            event.matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(j));
            event.matrixStack.translate(0.0D, 1.5F + h / 2.0F, 0.0D);
            event.matrixStack.multiply(new Quaternion(new Vector3f(SINE_45_DEGREES, 0.0F, SINE_45_DEGREES), crystalsRotationAngle.get().floatValue(), true));
            event.frame.render(event.matrixStack, vertexConsumer, event.light, renderLayer, frameColor.r / 255f, frameColor.g / 255f, frameColor.b / 255f, frameColor.a / 255f);
        }

        if (renderFrame.get()) {
            event.matrixStack.scale(0.875F, 0.875F, 0.875F);
            event.matrixStack.multiply(new Quaternion(new Vector3f(SINE_45_DEGREES, 0.0F, SINE_45_DEGREES), crystalsRotationAngle.get().floatValue(), true));
            event.matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(j));
            event.frame.render(event.matrixStack, vertexConsumer, event.light, renderLayer, frameColor.r / 255f, frameColor.g / 255f, frameColor.b / 255f, frameColor.a / 255f);
        }

        if (renderCore.get()) {
            event.matrixStack.scale(0.875F, 0.875F, 0.875F);
            event.matrixStack.multiply(new Quaternion(new Vector3f(SINE_45_DEGREES, 0.0F, SINE_45_DEGREES), crystalsRotationAngle.get().floatValue(), true));
            event.matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(j));
            event.core.render(event.matrixStack, vertexConsumer, event.light, renderLayer, coreColor.r / 255f, coreColor.g / 255f, coreColor.b / 255f, coreColor.a / 255f);
        }

        event.matrixStack.pop();

        BlockPos blockPos = event.endCrystalEntity.getBeamTarget();
        if (blockPos != null) {
            float m = (float)blockPos.getX() + 0.5F;
            float n = (float)blockPos.getY() + 0.5F;
            float o = (float)blockPos.getZ() + 0.5F;
            float p = (float)((double)m - event.endCrystalEntity.getX());
            float q = (float)((double)n - event.endCrystalEntity.getY());
            float r = (float)((double)o - event.endCrystalEntity.getZ());
            event.matrixStack.translate(p, q, r);
            EnderDragonEntityRenderer.renderCrystalBeam(-p, -q + h, -r, event.tickDelta, event.endCrystalEntity.endCrystalAge, event.matrixStack, event.vertexConsumerProvider, event.light);
        }

        event.matrixStack.pop();
    }

    private float getYOffset(EndCrystalEntity crystal, float tickDelta) {
        float f = (float) crystal.endCrystalAge + tickDelta;
        float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F * crystalsBounce.get().floatValue();
        return g - 1.4F;
    }
}