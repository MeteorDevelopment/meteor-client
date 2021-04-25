/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.misc.FakeClientPlayer;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class PlayerModelHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of player model.")
            .defaultValue(3)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    private final Setting<Boolean> copyYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-yaw")
            .description("Makes the player model's yaw equal to yours.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> copyPitch = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-pitch")
            .description("Makes the player model's pitch equal to yours.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> customYaw = sgGeneral.add(new IntSetting.Builder()
            .name("custom-yaw")
            .description("Custom yaw for when copy yaw is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    private final Setting<Integer> customPitch = sgGeneral.add(new IntSetting.Builder()
            .name("custom-pitch")
            .description("Custom pitch for when copy pitch is off.")
            .defaultValue(0)
            .min(-180)
            .max(180)
            .sliderMin(-180)
            .sliderMax(180)
            .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
            .name("background")
            .description("Displays a background behind the player model.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .description("Color of background.")
            .defaultValue(new SettingColor(0, 0, 0, 64))
            .build()
    );

    public PlayerModelHud(HUD hud) {
        super(hud, "player-model", "Displays a model of your player.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(50 * scale.get(), 75 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (background.get()) {
            Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            Renderer.NORMAL.quad(x, y, box.width, box.height, backgroundColor.get());
            Renderer.NORMAL.end();
        }

        PlayerEntity player = mc.player;
        if (isInEditor()) player = FakeClientPlayer.getPlayer();

        float yaw = copyYaw.get() ? MathHelper.wrapDegrees(player.prevYaw + (player.yaw - player.prevYaw) * mc.getTickDelta()) : (float) customYaw.get();
        float pitch = copyPitch.get() ? player.pitch : (float) customPitch.get();

        InventoryScreen.drawEntity((int) (x + (25 * scale.get())), (int) (y + (66 * scale.get())), (int) (30 * scale.get()), -yaw, -pitch, player);
    }
}
