package minegame159.meteorclient.modules.movement;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public NoFall() {
        super(Category.Movement, "no-fall", "Protects you from fall damage.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mc.player.fallDistance > 2f && !mc.player.isFallFlying()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
        }
    }
}
