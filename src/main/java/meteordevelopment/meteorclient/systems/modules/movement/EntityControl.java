/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class EntityControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Target entities.")
        .defaultValue()
        .build()
    );

    private final Setting<Boolean> maxJump = sgGeneral.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> saddleSpoof = sgGeneral.add(new BoolSetting.Builder()
        .name("saddle-spoof")
        .description("Lets you control rideable entities without a saddle.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> entitySpeed = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-speed")
        .description("Makes you go faster when riding entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .visible(entitySpeed::get)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .visible(entitySpeed::get)
        .build()
    );

    private final Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed only not in water.")
        .defaultValue(false)
        .visible(entitySpeed::get)
        .build()
    );

    private final Setting<Boolean> fly = sgGeneral.add(new BoolSetting.Builder()
        .name("fly")
        .description("Lets you fly with entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .visible(fly::get)
        .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.1)
        .min(0)
        .visible(fly::get)
        .build()
    );
    private final Setting<Boolean> cancelServerPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming move packets.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Locks the entity yaw.")
        .defaultValue(false)
        .build()
    );

    private boolean inBoat = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Entity e = mc.player.getVehicle();

        if (e != null) {
            if (!entities.get().getBoolean(e.getType())) return;
            if (lockYaw.get()) e.setYaw(mc.player.getYaw());
            if (e instanceof LlamaEntity) ((LlamaEntity) e).headYaw = mc.player.headYaw;

            inBoat = e instanceof BoatEntity;

            Vec3d eVel = e.getVelocity();

            double velX = eVel.x;
            double velY = eVel.y;
            double velZ = eVel.z;

            // Horizontal movement
            o:
            if (entitySpeed.get()) {
                Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
                if (!inBoat) {
                    if (onlyOnGround.get() && !e.isOnGround()) break o;
                    if (!inWater.get() && e.isTouchingWater()) break o;
                }
                velX = vel.getX();
                velZ = vel.getZ();
            }

            // Vertical movement
            if (fly.get()) {
                velY = 0;
                if (mc.options.jumpKey.isPressed()) velY += verticalSpeed.get() / 20;
                if (mc.options.sprintKey.isPressed()) velY -= verticalSpeed.get() / 20;
                else velY -= fallSpeed.get() / 20;
            }

            // Apply velocity
            e.setVelocity(velX, velY, velZ);

            if (maxJump.get()) ((ClientPlayerEntityAccessor) mc.player).setMountJumpStrength(1);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }

    public boolean boatFly() {
        return isActive() && inBoat;
    }

    public boolean saddleSpoof() {
        return isActive() && saddleSpoof.get();
    }

    public EntityControl() {
        super(Categories.Movement, "entity-control", "Control rideable entities.");
    }
}
