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
    private float pitch;

    public Pitch40() {
        super(ElytraFlightModes.Pitch40);
    }

    @Override
    public void onActivate() {
        if (mc.player.getY() < elytraFly.pitch40upperBounds.get()) {
            elytraFly.error("Player must be above upper bounds!");
            elytraFly.toggle();
        } else if (mc.player.getY() - 40 < elytraFly.pitch40lowerBounds.get()) {
            elytraFly.error("Player must be at least 40 blocks above the lower bounds!");
            elytraFly.toggle();
        }

        pitch = 32.0F;
    }

    /**
     * Create a random pitch around `pitch` that is within +/- bound/2
     * @param pitch the input pitch
     * @param bound the amount of variance
     * @return the changed pitch
     */
    private float randPitch(float pitch, float bound) {
        return (float) (pitch + (bound * (Math.random() - 0.5)));
    }

    @Override
    public void onTick() {
        super.onTick();

        /*
        When descending, look at 32-33 deg
        When ascending, look up at -49 at 10*pitch speed, then lower at 0.5 deg/tick until at 32-33
         */

        if (pitchingDown && mc.player.getY() <= elytraFly.pitch40lowerBounds.get()) {
            pitchingDown = false;
        }
        else if (!pitchingDown && mc.player.getY() >= elytraFly.pitch40upperBounds.get()) {
            pitchingDown = true;
        }

        // Pitch upwards
        if (!pitchingDown) {
            pitch -= randPitch(elytraFly.pitch40rotationSpeed.get().floatValue(), 3.0F);

            if (pitch < -49.0) {
                pitch = -49.0F;
                pitchingDown = true;
            }
        // Pitch downwards
        } else if (pitch < 32.0) {
            pitch += randPitch(0.5F, 0.125F);
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
