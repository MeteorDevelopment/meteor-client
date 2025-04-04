/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.render;

import motordevelopment.motorclient.events.render.Render2DEvent;
import motordevelopment.motorclient.renderer.Renderer2D;
import motordevelopment.motorclient.renderer.text.TextRenderer;
import motordevelopment.motorclient.settings.DoubleSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.Utils;
import motordevelopment.motorclient.utils.network.Http;
import motordevelopment.motorclient.utils.network.MotorExecutor;
import motordevelopment.motorclient.utils.render.NametagUtils;
import motordevelopment.motorclient.utils.render.color.Color;
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
        MotorExecutor.execute(() -> {
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
