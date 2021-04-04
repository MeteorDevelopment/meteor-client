/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextRadarHud extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
            .name("distance")
            .description("Shows the distance to the player next to their name.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("display-friends")
            .description("Whether to show friends or not.")
            .defaultValue(true)
            .build()
    );

    private final List<AbstractClientPlayerEntity> players = new ArrayList<>();

    public TextRadarHud(HUD hud) {
        super(hud, "player-info", "Displays players in your visual range.");
    }

    @Override
    public void update(HudRenderer renderer) {
        double width = renderer.textWidth("Players:");
        double height = renderer.textHeight();

        if (mc.world == null) {
            box.setSize(width, height);
            return;
        }

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().contains(Friends.get().get(entity))) continue;

            String text = entity.getGameProfile().getName();
            if (distance.get()) text += String.format("(%sm)", Math.round(mc.getCameraEntity().distanceTo(entity)));

            width = Math.max(width, renderer.textWidth(text));
            height += renderer.textHeight() + 2;
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        renderer.text("Players:", x, y, hud.secondaryColor.get());

        if (mc.world == null) return;

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().contains(Friends.get().get(entity))) continue;

            x = box.getX();
            y += renderer.textHeight() + 2;

            String text = entity.getGameProfile().getName();
            Color color = Friends.get().contains(Friends.get().get(entity)) ? Friends.get().getFriendColor(entity) : hud.primaryColor.get();

            renderer.text(text, x, y, color);

            if (distance.get()) {
                x += renderer.textWidth(text + " ");

                text = String.format("(%sm)", Math.round(mc.getCameraEntity().distanceTo(entity)));
                color = hud.secondaryColor.get();

                renderer.text(text, x, y, color);
            }
        }
    }

    private List<AbstractClientPlayerEntity> getPlayers() {
        players.clear();
        players.addAll(mc.world.getPlayers());
        players.sort(Comparator.comparingDouble(e -> e.distanceTo(mc.getCameraEntity())));
        return players;
    }
}