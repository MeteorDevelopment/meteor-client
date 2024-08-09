/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;

public class SafeWalk extends Module {
    private final SettingGroup swGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sneak = swGeneral.add(new BoolSetting.Builder()
            .name("sneak")
            .description("Sneak when approaching edge of block.")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> safeSneak = swGeneral.add(new BoolSetting.Builder()
            .name("safe-sneak")
            .description("Prevent you to falling if sneak doesn't trigger correctly.")
            .defaultValue(true)
            .visible(() -> this.sneak.get())
            .build());

    private final Setting<Boolean> sneakSprint = swGeneral.add(new BoolSetting.Builder()
            .name("sneak-on-sprint")
            .description("Keep sneak on sprinting key pressed.")
            .defaultValue(true)
            .visible(() -> this.sneak.get())
            .build());

    private final Setting<Double> edgeDistance = swGeneral.add(new DoubleSetting.Builder()
            .name("edge-distance")
            .description("Distance offset before reaching an edge.")
            .defaultValue(0.30)
            .sliderRange(0.00, 0.30)
            .decimalPlaces(2)
            .visible(() -> this.sneak.get())
            .build());

    private final Setting<Boolean> renderEdgeDistance = swGeneral.add(new BoolSetting.Builder()
            .name("render")
            .description("Render edge distance helper.")
            .defaultValue(false)
            .visible(() -> this.sneak.get())
            .build());

    private final Setting<Boolean> renderPlayerBox = swGeneral.add(new BoolSetting.Builder()
            .name("render-player-box")
            .description("Render player box helper.")
            .defaultValue(true)
            .visible(() -> this.sneak.get())
            .build());

    public SafeWalk() {
        super(Categories.Movement, "safe-walk", "Prevents you from walking off blocks.");
    }

    @EventHandler
    private void onClipAtLedge(ClipAtLedgeEvent event) {
        if (this.sneak.get()) {
            boolean closeToEdge = false;
            boolean isSprinting = this.sneakSprint.get() ? false : mc.options.sprintKey.isPressed();

            Box playerBox = mc.player.getBoundingBox();
            Box adjustedBox = this.getAdjustedPlayerBox(playerBox);

            if (mc.world.isSpaceEmpty(mc.player, adjustedBox) && mc.player.isOnGround())
                closeToEdge = true;

            if (!isSprinting) {
                if (closeToEdge == true) {
                    mc.player.input.sneaking = true;
                } else if (this.safeSneak.get()) {
                    event.setClip(true);
                }
            }
        } else {
            if (!mc.player.isSneaking())
                event.setClip(true);
        }

    }

    private Box getAdjustedPlayerBox(Box playerBox) {
        return playerBox.stretch(0, -mc.player.getStepHeight(), 0)
                .expand(-edgeDistance.get(), 0, -edgeDistance.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (this.sneak.get() && this.renderEdgeDistance.get()) {
            Box playerBox = mc.player.getBoundingBox();
            Box adjustedBox = getAdjustedPlayerBox(playerBox);

            event.renderer.box(adjustedBox, Color.BLUE, Color.RED, ShapeMode.Lines, 0);

            if (this.renderPlayerBox.get()) {
                event.renderer.box(playerBox, Color.BLUE, Color.GREEN, ShapeMode.Lines, 0);
            }
        }

    }
}
