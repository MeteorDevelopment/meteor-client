/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlayerMoveEvent;
import minegame159.meteorclient.mixininterface.ILookBehavior;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Speed extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How fast you want to go in blocks per second.")
            .defaultValue(5.6)
            .min(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Use speed only when on ground.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
            .name("in-water")
            .description("Use speed when in water.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> whenSneaking = sgGeneral.add(new BoolSetting.Builder()
            .name("when-sneaking")
            .description("Use speed when sneaking.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> applySpeedPotions = sgGeneral.add(new BoolSetting.Builder()
            .name("apply-speed-potions")
            .description("Apply speed potion effect.")
            .defaultValue(true)
            .build()
    );

    public Speed() {
        super(Category.Movement, "speed", "Speeeeeed.");
    }

    @EventHandler
    private final Listener<PlayerMoveEvent> onPlayerMove = new Listener<>(event -> {
        if (event.type != MovementType.SELF || mc.player.isFallFlying() || mc.player.isClimbing() || mc.player.getVehicle() != null) return;
        if (!whenSneaking.get() && mc.player.isSneaking()) return;
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if (!inWater.get() && mc.player.isTouchingWater()) return;

        float yaw = mc.player.yaw;
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            if (((ILookBehavior) BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior()).getTarget() != null) {
                yaw = ((ILookBehavior) BaritoneAPI.getProvider().getPrimaryBaritone().getLookBehavior()).getTarget().getYaw();
            }
        }

        Vec3d forward = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        double velX = 0;
        double velZ = 0;

        boolean a = false;
        if (mc.player.input.pressingForward) {
            velX += forward.x / 20 * speed.get();
            velZ += forward.z / 20 * speed.get();
            a = true;
        }
        if (mc.player.input.pressingBack) {
            velX -= forward.x / 20 * speed.get();
            velZ -= forward.z / 20 * speed.get();
            a = true;
        }

        boolean b = false;
        if (mc.player.input.pressingRight) {
            velX += right.x / 20 * speed.get();
            velZ += right.z / 20 * speed.get();
            b = true;
        }
        if (mc.player.input.pressingLeft) {
            velX -= right.x / 20 * speed.get();
            velZ -= right.z / 20 * speed.get();
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (applySpeedPotions.get() && mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            double value = (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1) * 0.205;
            velX += velX * value;
            velZ += velZ * value;
        }

        Anchor anchor = ModuleManager.INSTANCE.get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
    });
}
