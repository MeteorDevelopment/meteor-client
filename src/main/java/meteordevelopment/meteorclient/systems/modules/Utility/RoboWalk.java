package meteordevelopment.meteorclient.systems.modules.Utility;

import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixin.VehicleMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;

public class RoboWalk extends Module {
    public RoboWalk() {
        super(Categories.Utility, "robo-walk", "Bypasses LiveOverflow movement check.");
    }

    private double smooth(double d) {
        double temp = (double) Math.round(d * 100) / 100;
        return Math.nextAfter(temp, temp + Math.signum(d));
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (!packet.changesPosition()) return;

            double x = smooth(packet.getX(0));
            double z = smooth(packet.getZ(0));

            ((PlayerMoveC2SPacketAccessor) packet).setX(x);
            ((PlayerMoveC2SPacketAccessor) packet).setZ(z);
        } else if (event.packet instanceof VehicleMoveC2SPacket packet) {
            double x = smooth(packet.getX());
            double z = smooth(packet.getZ());

            ((VehicleMoveC2SPacketAccessor) packet).setX(x);
            ((VehicleMoveC2SPacketAccessor) packet).setZ(z);
        }
    }
}
