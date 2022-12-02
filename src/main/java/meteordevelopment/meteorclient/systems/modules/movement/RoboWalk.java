/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
// Done by: AtomicGamer9523
// For use in LiveOverflow's Minecraft server

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.Packet;
import net.minecraft.entity.Entity;

public class RoboWalk extends Module {

    private MinecraftClient instance = MinecraftClient.getInstance();
    public boolean ACTIVE = false;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug")
        .description("If debug info should be logged.")
        .defaultValue(false)
        .build()
    );

    public RoboWalk() {
        super(Categories.Movement, "robo-walk", "Allows you to move around like a robot. Used for LiveOverflow's Minecraft server.");
    }

    @Override public void onActivate() { ACTIVE = true; }

    @Override public void onDeactivate() { ACTIVE = false; }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if(event.packet instanceof PlayerMoveC2SPacket packet){
            if(packet instanceof PlayerMoveC2SPacket.PositionAndOnGround || packet instanceof PlayerMoveC2SPacket.Full){
                if(ACTIVE){
                    event.cancel();
                    double x = Math.round(packet.getX(0) * 100.0) / 100.0; //round packets as best we can
                    double z = Math.round(packet.getZ(0) * 100.0) / 100.0;


                    if(debug.get()) MeteorClient.LOG.debug("Sent [X/Z]: " + x + ":" + z);

                    long dx = ((long)(x * 1000)) % 10; //simulate the check that liveoverflow runs 
                    long dz = ((long)(z * 1000)) % 10;
                    if(debug.get()) MeteorClient.LOG.debug("Calculated [X/Z]: " + dx + ":" + dz);


                    if(dx != 0 || dz != 0){
                        if(debug.get()) MeteorClient.LOG.debug("Found packet [DX/DZ] != 0, Modification Failed!, Aborting!!"); //drop these weird packets that sometimes get through 
                        return;
                    }

                    Packet<?> clone;

                    if(packet instanceof PlayerMoveC2SPacket.PositionAndOnGround){
                        clone = new PlayerMoveC2SPacket.PositionAndOnGround(x, packet.getY(0), z, packet.isOnGround());
                    } else { 
                        clone = new PlayerMoveC2SPacket.Full(x, packet.getY(0), z, packet.getYaw(0), packet.getPitch(0), packet.isOnGround());
                    }

                    event.packet = clone;
                    instance.player.networkHandler.getConnection().send(clone);
                }
            }   
        }
        if(event.packet instanceof VehicleMoveC2SPacket packet){
            if(ACTIVE){
                event.cancel();
                double x = Math.round(packet.getX() * 100.0) / 100.0; //round packets as best we can
                double z = Math.round(packet.getZ() * 100.0) / 100.0;


                if(debug.get()) MeteorClient.LOG.debug("Sent [Vehicle] [X/Z]: " + x + ":" + z); //Main.debug log

                long dx = ((long)(x * 1000)) % 10; //simulate the check that liveoverflow runs 
                long dz = ((long)(z * 1000)) % 10;
                if(debug.get()) MeteorClient.LOG.debug("Calculated [Vehicle] [X/Z]: " + dx + ":" + dz);


                if(dx != 0 || dz != 0){
                    if(debug.get()) MeteorClient.LOG.debug("Found packet [Vehicle] [DX/DZ] != 0, Modification Failed!, Aborting!!"); //drop these weird packets that sometimes get through 
                    return;
                }

                Entity vehicle = instance.player.getVehicle();

                vehicle.setPos(x, packet.getY(), z);

                VehicleMoveC2SPacket movepacket = new VehicleMoveC2SPacket(vehicle);

                instance.player.networkHandler.getConnection().send(movepacket);
            }
        }
    }
}
