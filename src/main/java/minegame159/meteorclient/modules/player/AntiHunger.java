package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiHunger extends Module {
    public AntiHunger() {
        super(Category.Player, "anti-hunger", "Lose ur food slower. WARNING: It will u when u disable it.");
    }

    @EventHandler
    private Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (mc.player.fallDistance <= 0.0 && !mc.interactionManager.isBreakingBlock() && event.packet instanceof PlayerMoveC2SPacket) {
            ((IPlayerMoveC2SPacket) event.packet).setOnGround(false);
        }
    });
}
