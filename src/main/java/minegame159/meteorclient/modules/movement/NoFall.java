package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;

import java.lang.reflect.Field;

public class NoFall extends Module {
    private Field onGround;

    public NoFall() {
        super(Category.Movement, "no-fall", "Protects you from fall damage.");

        try {
            onGround = PlayerMoveC2SPacket.class.getDeclaredField("onGround");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    private void onSendPacket(SendPacketEvent e) {
        if (e.packet instanceof PlayerMoveC2SPacket) {
            try {
                onGround.set(e.packet, true);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }
}
