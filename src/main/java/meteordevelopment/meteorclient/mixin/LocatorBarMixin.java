/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import java.util.UUID;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterLocator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickManager;
import net.minecraft.world.waypoint.EntityTickProgress;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.Waypoint;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private static Identifier ARROW_UP;
    @Shadow @Final private static Identifier ARROW_DOWN;
    @Shadow @Final private static Identifier BACKGROUND;
    private static final Identifier XP_BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
    private static final Identifier XP_PROGRESS = Identifier.ofVanilla("hud/experience_bar_progress");

    private int getCenterX(Window window) {
        return (window.getScaledWidth() - 182) / 2;
    }

    private int getCenterY(Window window) {
        return window.getScaledHeight() - 24 - 5;
    }

    @Overwrite
    public void renderBar(DrawContext context, RenderTickCounter tickCounter) {
        BetterLocator locatorHud = Modules.get().get(BetterLocator.class);
        boolean showXP = locatorHud != null && locatorHud.isActive() && locatorHud.alwaysShowExperience.get();
        int i = this.getCenterX(this.client.getWindow());
        int j = this.getCenterY(this.client.getWindow());

        if (showXP) {
            ClientPlayerEntity clientPlayerEntity = this.client.player;
            int k = clientPlayerEntity.getNextLevelExperience();
            if (k > 0) {
                int l = (int)(clientPlayerEntity.experienceProgress * 183.0F);
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_BACKGROUND, i, j, 182, 5);
                if (l > 0) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_PROGRESS, 182, 5, 0, 0, i, j, l, 5);
                }
            } else {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 182, 5);
            }
        } else {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 182, 5);
        }

        if (locatorHud != null && locatorHud.isActive() && locatorHud.showDirections.get()) {
            float yaw = MathHelper.wrapDegrees(this.client.gameRenderer.getCamera().getYaw());
            int centerX = i + 182 / 2;
            int centerY = j;

            drawDirection(context, "S", 0, yaw, centerX, centerY);
            drawDirection(context, "W", 90, yaw, centerX, centerY);
            drawDirection(context, "N", 180, yaw, centerX, centerY);
            drawDirection(context, "E", -90, yaw, centerX, centerY);
        }
    }

    private void drawDirection(DrawContext context, String text, float dirYaw, float playerYaw, int centerX, int y) {
        float relative = MathHelper.wrapDegrees(dirYaw - playerYaw);
        if (relative >= -60 && relative <= 60) {
            int x = centerX + MathHelper.floor(relative * 173.0 / 2.0 / 60.0);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float)x, (float)y);
            context.getMatrices().scale(0.65f, 0.65f);

            int width = this.client.textRenderer.getWidth(text);
            context.drawText(this.client.textRenderer, text, -width / 2, 0, 0xFFFFFFFF, true);

            context.getMatrices().popMatrix();
        }
    }

    @Overwrite
    public void renderAddons(DrawContext context, RenderTickCounter tickCounter) {
        int i = this.getCenterY(this.client.getWindow());
        Entity entity = this.client.getCameraEntity();
        if (entity != null) {
            World world = entity.getEntityWorld();
            TickManager tickManager = world.getTickManager();
            EntityTickProgress entityTickProgress = entityx -> tickCounter.getTickProgress(!tickManager.shouldSkipTick(entityx));

            BetterLocator locatorHud = Modules.get().get(BetterLocator.class);
            boolean showHeads = locatorHud != null && locatorHud.isActive() && locatorHud.displayHeads.get();
            boolean showData = locatorHud != null && locatorHud.isActive() && locatorHud.displayData.get();
            boolean onlyTab = showData && locatorHud.displayOnlyOnTab.get();
            boolean showName = showData && locatorHud.displayName.get();
            boolean showCoords = showData && locatorHud.displayCoords.get();

            boolean shouldShowData = showData && (!onlyTab || this.client.options.playerListKey.isPressed());

            this.client.player.networkHandler.getWaypointHandler().forEachWaypoint(
                entity,
                waypoint -> {
                    if (!(Boolean)waypoint.getSource().left().map(uuid -> uuid.equals(entity.getUuid())).orElse(false)) {
                        double d = waypoint.getRelativeYaw(world, this.client.gameRenderer.getCamera(), entityTickProgress);
                        if (!(d <= -60.0) && !(d > 60.0)) {
                            int j = MathHelper.ceil((context.getScaledWindowWidth() - 9) / 2.0F);
                            int l = MathHelper.floor(d * 173.0 / 2.0 / 60.0);

                            float dist = MathHelper.sqrt((float)waypoint.squaredDistanceTo(entity));
                            Waypoint.Config config = waypoint.getConfig();
                            WaypointStyleAsset style = this.client.getWaypointStyleAssetManager().get(config.style);

                            float near = style.nearDistance();
                            float far = style.farDistance();
                            float progress = 1.0f - MathHelper.clamp((dist - near) / (far - near), 0.0f, 1.0f);

                            float distanceScale = MathHelper.lerp(progress, 0.5f, 1.0f);

                            float w = 9 * distanceScale;
                            float h = 9 * distanceScale;
                            float x = j + l - (w - 9) / 2;
                            float y = i - 2 - (h - 9) / 2;

                            boolean renderedHead = false;
                            if (showHeads && waypoint.getSource().left().isPresent()) {
                                UUID uuid = waypoint.getSource().left().get();
                                PlayerListEntry entry = this.client.getNetworkHandler().getPlayerListEntry(uuid);
                                if (entry != null) {
                                    Identifier skin = entry.getSkinTextures().body().texturePath();

                                    context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, (int)x, (int)y, 8.0f, 8.0f, (int)w, (int)h, 8, 8, 64, 64);
                                    context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, (int)x, (int)y, 40.0f, 8.0f, (int)w, (int)h, 8, 8, 64, 64);
                                    renderedHead = true;

                                    if (shouldShowData) {
                                        context.getMatrices().pushMatrix();
                                        context.getMatrices().translate(x + w / 2, y - 5);
                                        context.getMatrices().scale(0.5f, 0.5f);

                                        int textColor = -1;
                                        if (locatorHud.respectColor.get()) {
                                            textColor = (Integer)config.color
                                                .orElseGet(
                                                    () -> waypoint.getSource()
                                                        .map(
                                                            u -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, u.hashCode()), 0.9F),
                                                            n -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, n.hashCode()), 0.9F)
                                                        )
                                                );
                                        }

                                        if (showName) {
                                            Text name = entry.getDisplayName();
                                            if (name == null) name = Text.of(entry.getProfile().name());

                                            String text = name.getString();
                                            if (showCoords) {
                                                text += " (" + (int)dist + "m)";
                                            }

                                            int width = this.client.textRenderer.getWidth(text);
                                            context.drawTextWithBackground(this.client.textRenderer, Text.of(text), -width / 2, 0, width, textColor);
                                        } else if (showCoords) {
                                            String text = (int)dist + "m";
                                            int width = this.client.textRenderer.getWidth(text);
                                            context.drawTextWithBackground(this.client.textRenderer, Text.of(text), -width / 2, 0, width, textColor);
                                        }

                                        context.getMatrices().popMatrix();
                                    }
                                }
                            }

                            if (!renderedHead) {
                                Identifier identifier = style.getSpriteForDistance(dist);
                                int k = (Integer)config.color
                                    .orElseGet(
                                        () -> waypoint.getSource()
                                            .map(
                                                uuid -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, uuid.hashCode()), 0.9F),
                                                name -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, name.hashCode()), 0.9F)
                                            )
                                    );
                                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, (int)x, (int)y, (int)w, (int)h, k);
                            }

                            TrackedWaypoint.Pitch pitch = waypoint.getPitch(world, this.client.gameRenderer, entityTickProgress);
                            if (pitch != TrackedWaypoint.Pitch.NONE) {
                                int m;
                                Identifier identifier2;
                                if (pitch == TrackedWaypoint.Pitch.DOWN) {
                                    m = 6;
                                    identifier2 = ARROW_DOWN;
                                } else {
                                    m = -6;
                                    identifier2 = ARROW_UP;
                                }

                                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier2, j + l + 1, i + m, 7, 5);
                            }
                        }
                    }
                }
            );
        }
    }
}
