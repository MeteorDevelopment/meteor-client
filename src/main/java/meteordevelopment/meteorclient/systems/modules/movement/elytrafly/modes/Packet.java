/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Packet extends ElytraFlightMode {

    private final Vec3 vec3d = new Vec3d(0, 0, 0);

    public Packet() {
        super(ElytraFlightModes.Packet);
    }

    @Override
    public void onDeactivate() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().allowFlying = false;
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA || mc.player.fallDistance <= 0.2 || mc.options.sneakKey.isPressed())
            return;

        if (mc.options.forwardKey.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY(-(float) Math.toRadians(mc.player.getYaw()));
        } else if (mc.options.backKey.isPressed()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.rotateY((float) Math.toRadians(mc.player.getYaw()));
        }

        if (mc.options.jumpKey.isPressed()) {
            vec3d.add(0, elytraFly.verticalSpeed.get(), 0);
        } else if (!mc.options.jumpKey.isPressed()) {
            vec3d.add(0, -elytraFly.verticalSpeed.get(), 0);
        }

        mc.player.setVelocity(vec3d);
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.OnGroundOnly(true, mc.player.horizontalCollision));
    }

    //Walalalalalalalalalalalala
    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    @Override
    public void onPlayerMove() {
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(elytraFly.horizontalSpeed.get().floatValue() / 20);
    }
}
