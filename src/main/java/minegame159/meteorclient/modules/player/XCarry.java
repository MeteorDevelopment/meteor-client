package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IGuiCloseC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.network.packet.c2s.play.GuiCloseC2SPacket;

public class XCarry extends ToggleModule {
    private boolean invOpened;

    public XCarry() {
        super(Category.Player, "XCarry", "Allows you to store items in your crafting grid.");
    }

    @Override
    public void onActivate() {
        invOpened = false;
    }

    @Override
    public void onDeactivate() {
        if (invOpened) mc.player.networkHandler.sendPacket(new GuiCloseC2SPacket(mc.player.playerScreenHandler.syncId));
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof GuiCloseC2SPacket)) return;

        if (((IGuiCloseC2SPacket) event.packet).getSyncId() == mc.player.playerScreenHandler.syncId) {
            invOpened = true;
            event.cancel();
        }
    });
}
