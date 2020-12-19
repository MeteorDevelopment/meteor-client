/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import com.google.common.reflect.TypeToken;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends ToggleModule {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);
    private static final Type RESPONSE_TYPE = new TypeToken<List<UuidNameHistoryResponseItem>>() {}.getType();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the text.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Map<UUID, String> uuidToName = new HashMap<>();

    public EntityOwner() {
        super(Category.Render, "entity-owner", "Displays the name of the player who owns the entity you're looking at.");
    }

    @Override
    public void onDeactivate() {
        uuidToName.clear();
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Entity entity : mc.world.getEntities()) {
            UUID ownerUuid = null;
            if (entity instanceof TameableEntity) ownerUuid = ((TameableEntity) entity).getOwnerUuid();
            else if (entity instanceof HorseBaseEntity) ownerUuid = ((HorseBaseEntity) entity).getOwnerUuid();
            if (ownerUuid == null) continue;

            String name = getOwnerName(ownerUuid);
            renderNametag(event, entity, name);
        }
    });

    private void renderNametag(RenderEvent event, Entity entity, String name) {
        Camera camera = mc.gameRenderer.getCamera();

        // Compute scale
        double scale = 0.025 * this.scale.get();
        double dist = Utils.distanceToCamera(entity);
        if (dist > 8) scale *= dist / 8;

        // Setup the rotation
        Matrices.push();
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta + entity.getHeight() + 0.25;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;
        Matrices.translate(x - event.offsetX, y - event.offsetY, z - event.offsetZ);
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);
        Matrices.scale(-scale, -scale, scale);

        // Render background
        double ii = Fonts.get(2).getWidth(name) / 2.0;
        double i = ii * 0.25;
        Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
        Renderer.NORMAL.quad(-i - 1, 0, 0, -i - 1, 8, 0, i + 1, 8, 0, i + 1, 0, 0, BACKGROUND);
        Renderer.NORMAL.end();

        // Render name text
        Matrices.scale(0.25, 0.25, 1);
        Fonts.get(2).render(name, -ii, 0, TEXT);

        Matrices.pop();
    }

    private String getOwnerName(UUID uuid) {
// gets the name if the entity owner is online.
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player != null) return player.getGameProfile().getName();

        // Checks cache
        String name = uuidToName.get(uuid);
        if (name != null) return name;

// Makes a HTTP request to Mojang API. Fuck MineMonkey for pointing this out.
        MeteorExecutor.execute(() -> {
            if (isActive()) {
                List<UuidNameHistoryResponseItem> response = HttpUtils.get("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names", RESPONSE_TYPE);

                if (isActive()) {
                    if (response == null || response.size() <= 0) uuidToName.put(uuid, "Failed to get name");
                    else uuidToName.put(uuid, response.get(response.size() - 1).name);
                }
            }
        });

        name = "Retrieving";
        uuidToName.put(uuid, name);
        return name;
    }
}
