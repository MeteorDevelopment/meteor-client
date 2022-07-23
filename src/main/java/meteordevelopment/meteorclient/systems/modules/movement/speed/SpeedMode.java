/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class SpeedMode {
    protected final MinecraftClient mc;
    protected final Speed settings;
    private final SpeedModes type;

    protected int stage;
    protected double distance, speed;

    public SpeedMode(SpeedModes type) {
        this.settings = Modules.get().get(Speed.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
        reset();
    }

    public void onTick() {}
    public void onMove(PlayerMoveEvent event) {}
    public void onRubberband() {
        reset();
    }
    public void onActivate() {}
    public void onDeactivate() {}

    protected double getDefaultSpeed() {
        double defaultSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        return defaultSpeed;
    }

    protected void reset() {
        stage = 0;
        distance = 0;
        speed = 0.2873;
    }

    protected double getHop(double height) {
        StatusEffectInstance jumpBoost = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) ? mc.player.getStatusEffect(StatusEffects.JUMP_BOOST) : null;
        if (jumpBoost != null) height += (jumpBoost.getAmplifier() + 1) * 0.1f;
        return height;
    }

    public String getHudString() {
        return type.name();
    }
}
