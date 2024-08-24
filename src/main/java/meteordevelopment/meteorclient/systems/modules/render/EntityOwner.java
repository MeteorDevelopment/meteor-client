/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityOwner extends Module {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private static final Color TEXT = new Color(255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the text.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Vector3d pos = new Vector3d();
    private final Map<UUID, String> uuidToName = new HashMap<>();

    public EntityOwner() {
        super(Categories.Render, "entity-owner", "Displays the name of the player who owns the entity you're looking at.");
    }

    @Override
    public void onDeactivate() {
        uuidToName.clear();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entity entity : mc.world.getEntities()) {
            UUID ownerUuid;

            if (entity instanceof TameableEntity tameable) ownerUuid = tameable.getOwnerUuid();
            else continue;

            if (ownerUuid != null) {
                Utils.set(pos, entity, event.tickDelta);
                pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);

                if (NametagUtils.to2D(pos, scale.get())) {
                    renderNametag(getOwnerName(ownerUuid));
                }
            }
        }
    }

    private void renderNametag(String name) {
        TextRenderer text = TextRenderer.get();

        NametagUtils.begin(pos);
        text.beginBig();

        double w = text.getWidth(name);

        double x = -w / 2;
        double y = -text.getHeight();

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, w + 2, text.getHeight() + 2, BACKGROUND);
        Renderer2D.COLOR.render(null);

        text.render(name, x, y, TEXT);

        text.end();
        NametagUtils.end();
    }

    private String getOwnerName(UUID uuid) {
        // Check if the player is online
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player != null) return player.getName().getString();

        // Check cache
        String name = uuidToName.get(uuid);
        if (name != null) return name;

        // Makes an HTTP request to Mojang API
        MeteorExecutor.execute(() -> {
            if (isActive()) {
                ProfileResponse res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")).sendJson(ProfileResponse.class);

                if (isActive()) {
                    if (res == null) uuidToName.put(uuid, "Failed to get name");
                    else uuidToName.put(uuid, res.name);
                }
            }
        });

        name = "Retrieving";
        uuidToName.put(uuid, name);
        return name;
    }

    private static class ProfileResponse {
        public String name;
    }
}
