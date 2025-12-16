/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterLocator;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.TrackedWaypoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {
    @Shadow @Final private MinecraftClient client;

    @Unique private static final Identifier XP_BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
    @Unique private static final Identifier XP_PROGRESS = Identifier.ofVanilla("hud/experience_bar_progress");

    @Unique private BetterLocator module;

    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true)
    private void renderBarHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        module = Modules.get().get(BetterLocator.class);
        if (module == null || !module.isActive()) return;

        if (module.alwaysShowExperience.get()) {
            int maxExp = client.player.getNextLevelExperience();
            if (maxExp > 0) {
                int cx = getCenterX();
                int cy = getCenterY();
                int progress = (int) (client.player.experienceProgress * 183.0F);
                
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_BACKGROUND, cx, cy, 182, 5);
                if (progress > 0) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_PROGRESS, 182, 5, 0, 0, cx, cy, progress, 5);
                }
                
                renderOverlays(context, cx, cy);
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderBar", at = @At("RETURN"))
    private void renderBarReturn(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (module != null && module.isActive()) {
            renderOverlays(context, getCenterX(), getCenterY());
        }
    }

    @Unique
    private void renderOverlays(DrawContext context, int cx, int cy) {
        if (module.showDirections.get()) {
            float yaw = MathHelper.wrapDegrees(client.gameRenderer.getCamera().getYaw());
            int centerX = cx + 91;
            drawDirection(context, "S", 0, yaw, centerX, cy);
            drawDirection(context, "W", 90, yaw, centerX, cy);
            drawDirection(context, "N", 180, yaw, centerX, cy);
            drawDirection(context, "E", -90, yaw, centerX, cy);
        }
        renderMeteorWaypoints(context, cx + 91, cy);
    }

    @Unique
    private int getCenterX() {
        return (client.getWindow().getScaledWidth() - 182) / 2;
    }

    @Unique
    private int getCenterY() {
        return client.getWindow().getScaledHeight() - 29;
    }

    @Unique
    private void drawDirection(DrawContext context, String text, float dirYaw, float playerYaw, int centerX, int y) {
        float relative = MathHelper.wrapDegrees(dirYaw - playerYaw);
        if (relative >= -60 && relative <= 60) {
            renderText(context, text, centerX + MathHelper.floor(relative * 1.4416667), y, 0xFFFFFFFF);
        }
    }

    @Unique
    private void renderMeteorWaypoints(DrawContext context, int centerX, int centerY) {
        if (!module.displayWaypoints.get()) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        float playerYaw = MathHelper.wrapDegrees(client.gameRenderer.getCamera().getYaw());
        boolean showData = (module.displayWaypointName.get() || module.displayWaypointDistance.get()) && (!module.displayWaypointOnlyOnTab.get() || client.options.playerListKey.isPressed());

        for (Waypoint waypoint : Waypoints.get()) {
            if (!waypoint.visible.get() || !Waypoints.checkDimension(waypoint)) continue;

            BlockPos pos = waypoint.getPos();
            double dx = pos.getX() - cameraPos.x;
            double dz = pos.getZ() - cameraPos.z;
            
            double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90;
            float relative = MathHelper.wrapDegrees((float) angle - playerYaw);

            if (relative >= -60 && relative <= 60) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                int x = centerX + MathHelper.floor(relative * 1.4416667);
                int color = waypoint.color.get().getPacked();
                
                float progress = getProgress((float) dist, WaypointStyleAsset.DEFAULT_NEAR_DISTANCE, WaypointStyleAsset.DEFAULT_FAR_DISTANCE);
                float size = MathHelper.lerp(progress, 3.0f, 6.0f);
                int offset = (int) size / 2;
                int yOffset = centerY + 1 + (3 - (int)size) / 2;
                
                context.fill(x - offset, yOffset, x + offset + 1, yOffset + (int)size, color);

                if (showData) {
                    String text = module.displayWaypointName.get() ? waypoint.name.get() : "";
                    if (module.displayWaypointDistance.get()) text += (text.isEmpty() ? "" : " ") + "(" + (int) dist + "m)";
                    renderText(context, text, x + 0.5f, centerY - 5.0f, color);
                }
            }
        }
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void onDrawWaypointIcon(DrawContext context, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, int color, @Local(argsOnly = true) TrackedWaypoint waypoint) {
        if (w != 9 || h != 9 || module == null || !module.isActive() || client.getCameraEntity() == null || waypoint == null) {
            context.drawGuiTexture(pipeline, id, x, y, w, h, color);
            return;
        }

        PlayerListEntry entry = waypoint.getSource().left().map(uuid -> client.getNetworkHandler().getPlayerListEntry(uuid)).orElse(null);
        boolean showHeads = entry != null && module.displayHeads.get();
        
        float dist = MathHelper.sqrt((float) waypoint.squaredDistanceTo(client.getCameraEntity()));
        WaypointStyleAsset style = client.getWaypointStyleAssetManager().get(waypoint.getConfig().style);
        float scale = MathHelper.lerp(getProgress(dist, style.nearDistance(), style.farDistance()), 0.5f, 1.0f);
        float size = 9 * scale;
        float drawY = y - (size - 9) / 2;

        if (showHeads) {
             float drawX = x - (size - 9) / 2;
             Identifier skin = entry.getSkinTextures().body().texturePath();
             context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, (int) drawX, (int) drawY, 8.0f, 8.0f, (int) size, (int) size, 8, 8, 64, 64);
             context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, (int) drawX, (int) drawY, 40.0f, 8.0f, (int) size, (int) size, 8, 8, 64, 64);
        } else {
             context.drawGuiTexture(pipeline, id, x, y, w, h, color);
        }

        if (entry != null && module.displayPlayerData.get() && (!module.displayPlayerOnlyOnTab.get() || client.options.playerListKey.isPressed())) {
            int textColor = -1;
            if (module.respectPlayerColor.get()) {
                textColor = (Integer) waypoint.getConfig().color.orElseGet(() -> 
                    waypoint.getSource().map(
                        u -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, u.hashCode()), 0.9F),
                        n -> ColorHelper.withBrightness(ColorHelper.withAlpha(255, n.hashCode()), 0.9F)
                    )
                );
            }

            String text = "";
            if (module.displayPlayerName.get()) {
                Text name = entry.getDisplayName();
                text = (name != null ? name : Text.of(entry.getProfile().name())).getString();
                if (module.displayPlayerDistance.get()) text += " (" + (int) dist + "m)";
            } else {
                text = (int) dist + "m";
            }
            renderText(context, text, x + 4.5f, drawY - 5, textColor);
        }
    }

    @Unique
    private float getProgress(float dist, float near, float far) {
        return 1.0f - MathHelper.clamp((dist - near) / (far - near), 0.0f, 1.0f);
    }

    @Unique
    private void renderText(DrawContext context, String text, float x, float y, int color) {
        if (text.isEmpty()) return;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(0.5f, 0.5f);
        int width = client.textRenderer.getWidth(text);
        
        if (color == 0xFFFFFFFF) {
            context.drawText(client.textRenderer, text, -width / 2, 0, color, true);
        } else {
            context.drawTextWithBackground(client.textRenderer, Text.of(text), -width / 2, 0, width, color);
        }
        
        context.getMatrices().popMatrix();
    }
}
