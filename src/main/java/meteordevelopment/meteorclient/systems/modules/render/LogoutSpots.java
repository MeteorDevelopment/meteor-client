/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class LogoutSpots extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    public final Setting<Integer> lifetime = sgGeneral.add(new IntSetting.Builder()
        .name("lifetime")
        .description("Nametag lifetime in seconds. 0 to disable.")
        .defaultValue(300)
        .min(0)
        .sliderMax(1200)
        .build()
    );

    public final Setting<Integer> renderCount = sgGeneral.add(new IntSetting.Builder()
        .name("render-count")
        .description("Rendered nametag limit.")
        .defaultValue(50)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> showHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("show-health")
        .description("Show the player's health.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> logoutTime = sgGeneral.add(new BoolSetting.Builder()
        .name("logout-time")
        .description("Show the player's logout time in seconds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderText = sgGeneral.add(new BoolSetting.Builder()
        .name("render-text")
        .description("Display a nametag above player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode")
        .description("Display a player model.")
        .defaultValue(RenderMode.Wireframe)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(1)
        .min(0)
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
        lineColor.onChanged();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();

        timer = 10;
        lastDimension = PlayerUtils.getDimension();
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
            IntStream.range(0, players.size()).filter(i -> players.get(i).uuid.equals(event.entity.getUuid())).findFirst().ifPresent(players::remove);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        players.forEach(entry -> entry.ticks++);

        if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile())))
                    continue;

                lastPlayers.stream().filter(player -> player.getUuid().equals(entry.getProfile().getId())).map(Entry::new).forEach(this::add);
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

        Dimension dimension = PlayerUtils.getDimension();
        if (dimension != lastDimension) players.clear();
        lastDimension = dimension;

        players.removeIf(e -> lifetime.get() != 0 && e.ticks > lifetime.get() * 20);

        players.sort((a, b) -> b.ticks - a.ticks);
        if (players.size() > renderCount.get()) {
            IntStream.range(0, players.size()).findFirst().ifPresent(players::remove);
        }

    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        players.forEach(p -> p.render3D(event));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        players.forEach(Entry::render2D);
    }

    @Override
    public String getInfoString() {
        return Integer.toString(players.size());
    }

    private static final Vector3d pos = new Vector3d();

    private class Entry {
        public final double x, y, z;
        public final double xWidth, zWidth, halfWidth, height;

        public final PlayerEntity player;
        public final UUID uuid;
        public final String name;
        public final String healthText;
        public final Color healthColor;
        public int ticks = 0;

        public Entry(PlayerEntity entity) {
            this.player = entity;

            halfWidth = entity.getWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;

            xWidth = entity.getBoundingBox().getXLength();
            zWidth = entity.getBoundingBox().getZLength();
            height = entity.getBoundingBox().getYLength();

            uuid = entity.getUuid();
            name = entity.getEntityName();

            healthText = " " + Utils.getHealth(entity);
            healthColor = Utils.getHealthColor(entity);
        }

        public void render3D(Render3DEvent event) {
            if (!EntityUtils.isInRenderDistance(x, z)) return;
            switch (renderMode.get()) {
                case Wireframe ->
                    WireframeEntityRenderer.render(event, player, 1, sideColor.get(), lineColor.get(), shapeMode.get());
                case Box ->
                    event.renderer.box(x, y, z, x + xWidth, y + height, z + zWidth, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }

        public void render2D() {
            if (!renderText.get() || !EntityUtils.isInRenderDistance(x, z)) return;

            TextRenderer text = TextRenderer.get();
            double scale = LogoutSpots.this.scale.get();
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);

            if (!NametagUtils.to2D(pos, scale)) return;

            NametagUtils.begin(pos);

            String timerText = Utils.ticksToTime(ticks) + " ";

            // Render background
            double width = text.getWidth(name);
            double heightDown = text.getHeight();
            if (showHealth.get()) width += text.getWidth(healthText);
            if (logoutTime.get()) width += text.getWidth(timerText);
            double widthHalf = width / 2.0;

            RenderUtils.drawBg(-widthHalf, -heightDown, width, heightDown, nameBackgroundColor.get());

            // Render name and health texts
            text.beginBig();
            double hX = -widthHalf;
            double hY = -heightDown;
            if (logoutTime.get()) hX = text.render(timerText, hX, hY, nameColor.get());
            hX = text.render(name, hX, hY, nameColor.get());
            if (showHealth.get()) text.render(healthText, hX, hY, Utils.getHealthColor(player));
            text.end();

            NametagUtils.end();
        }
    }

    public enum RenderMode {
        None,
        Box,
        Wireframe
    }
}
