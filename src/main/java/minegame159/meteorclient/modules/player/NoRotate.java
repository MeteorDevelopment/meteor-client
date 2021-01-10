package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.combat.Quiver;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoRotate extends Module {

    public NoRotate() {
        super(Category.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            if (ModuleManager.INSTANCE.get(EXPThrower.class).isActive() || ModuleManager.INSTANCE.get(Quiver.class).isActive()) return;
            ((IPlayerMoveC2SPacket) event.packet).setPitch(mc.player.getPitch(0));
            ((IPlayerMoveC2SPacket) event.packet).setYaw(mc.player.getYaw(1));
        }
    });
}
