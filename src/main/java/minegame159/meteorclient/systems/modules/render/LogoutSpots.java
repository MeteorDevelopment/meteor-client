/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.*;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.Dimension;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends Module {
    private static final MeshBuilder MB = new MeshBuilder(64);

    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> fullHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("full-height")
            .description("Displays the height as the player's full height.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 0, 255, 55))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 0, 255))
            .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
            .name("name-color")
            .description("The name color.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
            .name("name-background-color")
            .description("The name background color.")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
    );

    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();

    private int timer;
    private Dimension lastDimension;

    public LogoutSpots() {
        super(Categories.Render, "logout-spots", "Displays a box where another player has logged out at.");
        lineColor.changed();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();

        timer = 10;
        lastDimension = Utils.getDimension();
    }

    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) lastPlayers.add((PlayerEntity) entity);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof PlayerEntity) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUuid())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile()))) continue;

                for (PlayerEntity player : lastPlayers) {
                    if (player.getUuid().equals(entry.getProfile().getId())) {
                        add(new Entry(player));
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
            updateLastPlayers();
        }

        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }

        Dimension dimension = Utils.getDimension();
        if (dimension != lastDimension) players.clear();
        lastDimension = dimension;
    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        for (Entry player : players) player.render(event);

        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        DiffuseLighting.disable();
        RenderSystem.enableBlend();
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private class Entry {
        public final double x, y, z;
        public final double xWidth, zWidth, height;

        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;

        public Entry(PlayerEntity entity) {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();

            xWidth = entity.getBoundingBox().getXLength();
            zWidth = entity.getBoundingBox().getZLength();
            height = entity.getBoundingBox().getYLength();

            uuid = entity.getUuid();
            name = entity.getGameProfile().getName();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());

            healthText = " " + health;
        }

        public void render(RenderEvent event) {
            Camera camera = mc.gameRenderer.getCamera();

            // Compute scale
            double scale = 0.025;
            double dist = Utils.distanceToCamera(x, y, z);
            if (dist > 8) scale *= dist / 8 * LogoutSpots.this.scale.get();

            if (dist > mc.options.viewDistance * 16) return;

            // Compute health things
            double healthPercentage = (double) health / maxHealth;

            // Render quad
            if (fullHeight.get()) Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, x + xWidth, y + height, z + zWidth, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            else Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x, y, z, xWidth, sideColor.get(), lineColor.get(), shapeMode.get());

            // Get health color
            Color healthColor;
            if (healthPercentage <= 0.333) healthColor = RED;
            else if (healthPercentage <= 0.666) healthColor = ORANGE;
            else healthColor = GREEN;

            // Setup the rotation
            Matrices.push();
            Matrices.translate(x + xWidth / 2 - event.offsetX, y + (fullHeight.get() ? (height + 0.5) : 0.5)  - event.offsetY, z + zWidth / 2 - event.offsetZ);
            Matrices.rotate(-camera.getYaw(), 0, 1, 0);
            Matrices.rotate(camera.getPitch(), 1, 0, 0);
            Matrices.scale(-scale, -scale, scale);

            // Render background
            double i = TextRenderer.get().getWidth(name) / 2.0 + TextRenderer.get().getWidth(healthText) / 2.0;
            MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            MB.quad(-i - 1, -1, 0, -i - 1, 8, 0, i + 1, 8, 0, i + 1, -1, 0, nameBackgroundColor.get());
            MB.end();

            // Render name and health texts
            TextRenderer.get().begin(1, false, true);
            double hX = TextRenderer.get().render(name, -i, 0, nameColor.get());
            TextRenderer.get().render(healthText, hX, 0, healthColor);
            TextRenderer.get().end();

            Matrices.pop();
        }
    }
}
