/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Dimension;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogoutSpots extends ToggleModule {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final Color GREEN = new Color(25, 225, 25);
    private static final Color ORANGE = new Color(225, 105, 25);
    private static final Color RED = new Color(225, 25, 25);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Color sideColor = new Color();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Color> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(255, 0, 255))
            .onChanged(color1 -> {
                sideColor.set(color1);
                sideColor.a -= 200;
                sideColor.validate();
            })
            .build()
    );

    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();

    private int timer;
    private Dimension lastDimension;

    public LogoutSpots() {
        super(Category.Render, "logout-spots", "Displays players logout position.");
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
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
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
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
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
    });

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Entry player : players) player.render(event);

        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        DiffuseLighting.disable();
        RenderSystem.enableBlend();
    });

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private class Entry {
        public final double x, y, z;
        public final double width, height;

        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;

        public Entry(PlayerEntity entity) {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();

            width = entity.getBoundingBox().getXLength();
            height = entity.getBoundingBox().getZLength();

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
            if (dist > 10) scale *= dist / 10 * LogoutSpots.this.scale.get();

            if (dist > mc.options.viewDistance * 16) return;

            // Compute health things
            double healthPercentage = (double) health / maxHealth;

            // Render quad
            ShapeBuilder.quadWithLines(x, y, z, width, height, sideColor, lineColor.get());

            // Get health color
            Color healthColor;
            if (healthPercentage <= 0.333) healthColor = RED;
            else if (healthPercentage <= 0.666) healthColor = ORANGE;
            else healthColor = GREEN;

            // Setup the rotation
            Matrices.push();
            Matrices.translate(x + width / 2 - event.offsetX, y + 0.5 - event.offsetY, z + height / 2 - event.offsetZ);
            Matrices.rotate(-camera.getYaw(), 0, 1, 0);
            Matrices.rotate(camera.getPitch(), 1, 0, 0);
            Matrices.scale(-scale, -scale, scale);

            // Render background
            double i = MeteorClient.FONT_2X.getStringWidth(name) / 2.0 + MeteorClient.FONT_2X.getStringWidth(healthText) / 2.0;
            ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            ShapeBuilder.quad(-i - 1, -1, 0, -i - 1, 8, 0, i + 1, 8, 0, i + 1, -1, 0, BACKGROUND);
            ShapeBuilder.end();

            // Render name and health texts
            MeteorClient.FONT_2X.begin();
            double hX = MeteorClient.FONT_2X.renderString(name, -i, 0, TEXT);
            MeteorClient.FONT_2X.renderString(healthText, hX, 0, healthColor);
            MeteorClient.FONT_2X.end();

            Matrices.pop();
        }
    }
}
