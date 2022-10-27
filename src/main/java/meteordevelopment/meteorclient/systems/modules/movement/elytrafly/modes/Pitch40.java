/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;

public class Pitch40 extends ElytraFlightMode {
    private boolean pitchingDown = true;
    private int pitch;

    public Pitch40() {
        super(ElytraFlightModes.Pitch40);
    }

    @Override
    public void onActivate() {
        if (mc.player.getY() < elytraFly.pitch40upperBounds.get()) {
            elytraFly.error("Player must be above upper bounds!");
            elytraFly.toggle();
        }

        pitch = 40;
    }

    @Override
    public void onDeactivate() {}

    @Override
    public void onTick() {
        super.onTick();

        if (pitchingDown && mc.player.getY() <= elytraFly.pitch40lowerBounds.get()) {
            pitchingDown = false;
        }
        else if (!pitchingDown && mc.player.getY() >= elytraFly.pitch40upperBounds.get()) {
            pitchingDown = true;
        }

        // Pitch upwards
        if (!pitchingDown && mc.player.getPitch() > -40) {
            pitch -= elytraFly.pitch40rotationSpeed.get();

            if (pitch < -40) pitch = -40;
        // Pitch downwards
        } else if (pitchingDown && mc.player.getPitch() < 40) {
            pitch += elytraFly.pitch40rotationSpeed.get();

            if (pitch > 40) pitch = 40;
        }

        mc.player.setPitch(pitch);
    }

    @Override
    public void autoTakeoff() {}

    @Override
    public void handleHorizontalSpeed(PlayerMoveEvent event) {
        velX = event.movement.x;
        velZ = event.movement.z;
    }

    @Override
    public void handleVerticalSpeed(PlayerMoveEvent event) {}

    @Override
    public void handleFallMultiplier() {}

    @Override
    public void handleAutopilot() {}
}
