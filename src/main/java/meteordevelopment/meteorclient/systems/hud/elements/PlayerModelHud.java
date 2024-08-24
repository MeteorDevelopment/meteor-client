/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerModelHud extends HudElement {
    public static final HudElementInfo<PlayerModelHud> INFO = new HudElementInfo<>(Hud.GROUP, "player-model", "Displays a model of your player.", PlayerModelHud::new);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .onChanged(aDouble -> calculateSize())
        .build()
    );

    private final Setting<Boolean> copyYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-yaw")
        .description("Makes the player model's yaw equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> customYaw = sgGeneral.add(new IntSetting.Builder()
        .name("custom-yaw")
        .description("Custom yaw for when copy yaw is off.")
        .defaultValue(0)
        .range(-180, 180)
        .sliderRange(-180, 180)
        .visible(() -> !copyYaw.get())
        .build()
    );

    private final Setting<Boolean> copyPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-pitch")
        .description("Makes the player model's pitch equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> customPitch = sgGeneral.add(new IntSetting.Builder()
        .name("custom-pitch")
        .description("Custom pitch for when copy pitch is off.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> !copyPitch.get())
        .build()
    );

    private final Setting<CenterOrientation> centerOrientation = sgGeneral.add(new EnumSetting.Builder<CenterOrientation>()
        .name("center-orientation")
        .description("Which direction the player faces when the HUD model faces directly forward.")
        .defaultValue(CenterOrientation.South)
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public PlayerModelHud() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.post(() -> {
            PlayerEntity player = mc.player;
            if (player == null) return;

            float offset = centerOrientation.get() == CenterOrientation.North ? 180 : 0;

            float yaw = copyYaw.get() ? MathHelper.wrapDegrees(player.prevYaw + (player.getYaw() - player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true) + offset) : (float) customYaw.get();
            float pitch = copyPitch.get() ? player.getPitch() : (float) customPitch.get();

            drawEntity(renderer.drawContext, x, y, (int) (30 * scale.get()), -yaw, -pitch, player);
        });

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        } else if (mc.player == null) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
            renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
            renderer.line(x + getWidth(), y, x, y + getHeight(), Color.GRAY);
        }
    }

    private void calculateSize() {
        setSize(50 * scale.get(), 75 * scale.get());
    }

    /**
     * Draws an entity to the screen. The default version provided by InventoryScreen has had its parameters changed
     * such that it's no longer appropriate for this use case. As the new version uses rotation based on the mouse
     * position relative to itself, it causes some odd angle positioning that may also look "stuck" to one corner,
     * and the model's facing may change depending on how we reposition the element.
     * Additionally, it uses OpenGL scissors, which causes the player model to get cut when the Minecraft GUI scale is not 1x.
     * This version of drawEntity should fix these issues.
     */
    private void drawEntity(DrawContext context, int x, int y, int size, float yaw, float pitch, LivingEntity entity) {

        float tanYaw = (float) Math.atan((yaw) / 40.0f);
        float tanPitch = (float) Math.atan((pitch) / 40.0f);

        // By default, the origin of the drawEntity command is the top-center, facing down and straight to the south.
        // This means that the player model is upside-down. We'll apply a rotation of PI radians (180 degrees) to fix this.
        // This does have the downside of setting the origin to the bottom-center corner, though, so we'll have
        // to compensate for this later.
        Quaternionf quaternion = new Quaternionf().rotateZ((float) Math.PI);

        // The drawEntity command draws the entity using some entity parameters, so we'll have to manipulate some of
        // those to draw as we want. But first, we'll save the previous values, so we can restore them later.
        float previousBodyYaw = entity.bodyYaw;
        float previousYaw = entity.getYaw();
        float previousPitch = entity.getPitch();
        float previousPrevHeadYaw = entity.prevHeadYaw; // A perplexing name, I know!
        float prevHeadYaw = entity.headYaw;

        // Apply the rotation parameters
        entity.bodyYaw = 180.0f + tanYaw * 20.0f;
        entity.setYaw(180.0f + tanYaw * 40.0f);
        entity.setPitch(-tanPitch * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();

        // Recall the player's origin is now the bottom-center corner, so we'll have to offset the draw by half the width
        // to get it to render in the center.
        // As for the y parameter, adding the element's height draws it at the bottom, but in practice we want the player
        // to "float" somewhat, so we'll multiply it by some constant to have it hover. It turns out 0.9 is a good value.
        // The vector3 parameter applies a translation to the player's model. Given that we're simply offsetting
        // the draw in the x and y parameters, we won't really need this, so we'll set it to default.
        // It doesn't seem like quaternionf2 does anything, so we'll leave it null to save some computation.
        InventoryScreen.drawEntity(context, x + getWidth() / 2, y + getHeight() * 0.9f, size, new Vector3f(), quaternion, null, entity);

        // Restore the previous values
        entity.bodyYaw = previousBodyYaw;
        entity.setYaw(previousYaw);
        entity.setPitch(previousPitch);
        entity.prevHeadYaw = previousPrevHeadYaw;
        entity.headYaw = prevHeadYaw;
    }

    private enum CenterOrientation {
        North,
        South
    }
}
