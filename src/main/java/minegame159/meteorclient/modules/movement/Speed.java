/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {

    public enum JumpIf {
        Sprinting,
        Walking,
        Always
    }

    public enum Mode {
        Jump,
        Velocity
    }

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
            .description("Use speed only when standing on a block.")
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
            .description("Apply the speed effect via potions.")
            .defaultValue(true)
            .build()
    );

    private final SettingGroup sgJump = settings.createGroup("Jump");

    private final Setting<Boolean> jump = sgJump.add(new BoolSetting.Builder()
            .name("jump")
            .description("Automatically jumps.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Mode> jumpMode = sgJump.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The method of jumping.")
            .defaultValue(Mode.Jump)
            .build()
    );

    private final Setting<Double> velocityHeight = sgJump.add(new DoubleSetting.Builder()
            .name("velocity-height")
            .description("The distance that velocity mode moves you.")
            .defaultValue(0.25)
            .min(0)
            .sliderMax(2)
            .build()
    );

    private final Setting<JumpIf> jumpIf = sgJump.add(new EnumSetting.Builder<JumpIf>()
            .name("jump-if")
            .description("Jump if.")
            .defaultValue(JumpIf.Walking)
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

        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        double velX = vel.getX();
        double velZ = vel.getZ();

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

    @EventHandler
    private final Listener<TickEvent.Pre> onPreTick = new Listener<>(event -> {
        if (jump.get()) {
            if (!mc.player.isOnGround() || mc.player.isSneaking() || !jump()) return;

            if (jumpMode.get() == Mode.Jump) mc.player.jump();
            else ((IVec3d) mc.player.getVelocity()).setY(velocityHeight.get());
        }
    });

    private boolean jump() {
        switch (jumpIf.get()) {
            case Sprinting: return mc.player.isSprinting() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0);
            case Walking:   return mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0;
            case Always:    return true;
            default:        return false;
        }
    }
}
