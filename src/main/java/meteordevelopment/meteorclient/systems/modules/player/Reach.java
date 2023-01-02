/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class Reach extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTpHit = settings.createGroup("TP Hit");

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
            .name("reach")
            .description("Your reach modifier.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Boolean> tpHit = sgTpHit.add(new BoolSetting.Builder()
            .name("TP Hit")
            .description("Attacks entities with short-range teleportation.")
            .build()
    );

    private final Setting<Double> blinkDistance = sgTpHit.add(new DoubleSetting.Builder()
            .name("blink-distance")
            .description("Distance to move every packet.")
            .defaultValue(9.5)
            .min(1)
            .sliderMax(10)
            .visible(tpHit::get)
            .build()
    );

    private final Setting<Integer> packetDelay = sgTpHit.add(new IntSetting.Builder()
            .name("packet-delay")
            .description("Delay between sending movement packets in milliseconds.")
            .defaultValue(45)
            .min(1)
            .visible(tpHit::get)
            .build()
    );

    private final Setting<Integer> packetAmount = sgTpHit.add(new IntSetting.Builder()
            .name("packet-amount")
            .description("How many packets to send at once.")
            .defaultValue(4)
            .min(1)
            .visible(tpHit::get)
            .build()
    );

    public Reach() {
        super(Categories.Player, "reach", "Gives you super long arms.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!tpHit.get()) return;
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;
        if (!mc.options.attackKey.isPressed()) return;

        Entity target = mc.targetedEntity;
        if (target == null) return;

        (new Thread(() -> {
            Vec3d initialPos = mc.player.getPos();

            sendMovePackets(initialPos, target.getPos());

            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

            sendMovePackets(target.getPos(), initialPos);
            mc.player.setPosition(initialPos);
        })).start();
    }

    public float getReach() {
        if (!isActive()) return mc.interactionManager.getCurrentGameMode().isCreative() ? 5.0F : 4.5F;
        return reach.get().floatValue();
    }

    private void sendMovePackets(Vec3d fromPos, Vec3d targetPos) {
        double targetDistance = Math.ceil(fromPos.distanceTo(targetPos) / blinkDistance.get());
        for (int i = 1; i <= targetDistance; i++) {
            Vec3d newPos = fromPos.lerp(targetPos, i / targetDistance);
            PlayerMoveC2SPacket movePacket = new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, false);
            mc.player.networkHandler.sendPacket(movePacket);

            if (i % packetAmount.get() == 0) {
                try {
                    Thread.sleep(packetDelay.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
