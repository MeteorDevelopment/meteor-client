/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class Packet extends ElytraFlightMode {

    private final Vec3 vec3d = new Vec3(0, 0, 0);

    public Packet() {
        super(ElytraFlightModes.Packet);
    }

    @Override
    public void onDeactivate() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().mayfly = false;
    }

    @Override
    public void onTick() {
        super.onTick();

        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA || mc.player.fallDistance <= 0.2 || mc.options.keyShift.isDown())
            return;

        if (mc.options.keyUp.isDown()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.yRot(-(float) Math.toRadians(mc.player.getYRot()));
        } else if (mc.options.keyDown.isDown()) {
            vec3d.add(0, 0, elytraFly.horizontalSpeed.get());
            vec3d.yRot((float) Math.toRadians(mc.player.getYRot()));
        }

        if (mc.options.keyJump.isDown()) {
            vec3d.add(0, elytraFly.verticalSpeed.get(), 0);
        } else if (!mc.options.keyJump.isDown()) {
            vec3d.add(0, -elytraFly.verticalSpeed.get(), 0);
        }

        mc.player.setDeltaMovement(vec3d);
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
    }

    //Walalalalalalalalalalalala
    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket) {
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    @Override
    public void onPlayerMove() {
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlyingSpeed(elytraFly.horizontalSpeed.get().floatValue() / 20);
    }
}
