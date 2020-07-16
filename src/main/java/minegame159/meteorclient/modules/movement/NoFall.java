package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends ToggleModule {
    public NoFall() {
        super(Category.Movement, "no-fall", "Protects you from fall damage.");
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            ((IPlayerMoveC2SPacket) event.packet).setOnGround(true);
        }
    });
}
