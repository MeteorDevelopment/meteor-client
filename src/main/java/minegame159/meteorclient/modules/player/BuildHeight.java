package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IBlockHitResult;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.Direction;

public class BuildHeight extends ToggleModule {
    public BuildHeight() {
        super(Category.Player, "build-height", "Lets you interact with blocks at build limit.");
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) event.packet;
        if (p.getHitY().getPos().y >= 255 && p.getHitY().getSide() == Direction.UP) {
            ((IBlockHitResult) p.getHitY()).setSide(Direction.DOWN);
        }
    });
}
