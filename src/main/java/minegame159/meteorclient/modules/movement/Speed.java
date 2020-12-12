/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlayerMoveEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class Speed extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("How fast the player moves in blocks per second.")
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
            .description("Use speed whilst inside water.")
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
            .description("Apply the speed effect.")
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
}
