/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import com.google.common.reflect.TypeToken;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.network.HttpUtils;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.network.UuidNameHistoryResponseItem;
import minegame159.meteorclient.utils.render.NametagUtils;
import minegame159.meteorclient.utils.render.color.Color;
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

public class EntityOwner extends Module {
    private static final MeshBuilder MB = new MeshBuilder(128);

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
        NametagUtils.begin(event, entity, scale.get());
        TextRenderer.get().begin(1, false, true);

        double w = TextRenderer.get().getWidth(name) / 2;
        double h = TextRenderer.get().getHeight();

        // Render background
        MB.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
        MB.quad(-w - 1, 0, 0, -w - 1, h, 0, w + 1, h, 0, w + 1, 0, 0, BACKGROUND);
        MB.end();

        // Render name text
        TextRenderer.get().render(name, -w, 0, TEXT);

        TextRenderer.get().end();
        NametagUtils.end();
    }

    private String getOwnerName(UUID uuid) {
        // Check if the player is online
        PlayerEntity player = mc.world.getPlayerByUuid(uuid);
        if (player != null) return player.getGameProfile().getName();

        // Check cache
        String name = uuidToName.get(uuid);
        if (name != null) return name;

        // Makes a HTTP request to Mojang API
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
