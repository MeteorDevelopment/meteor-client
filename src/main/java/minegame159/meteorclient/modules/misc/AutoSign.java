package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.ISignEditScreen;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.LiteralText;

public class AutoSign extends Module {
    private String[] text;

    public AutoSign() {
        super(Category.Misc, "auto-sign", "Automatically writes signs. When enabled first sign's text will be used.");
    }

    @Override
    public void onDeactivate() {
        text = null;
    }

    @EventHandler
    private Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof UpdateSignC2SPacket)) return;

        text = ((UpdateSignC2SPacket) event.packet).getText();
    });

    @EventHandler
    private Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> {
        if (!(event.screen instanceof SignEditScreen) || text == null) return;

        SignBlockEntity sign = ((ISignEditScreen) event.screen).getSign();

        mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), new LiteralText(text[0]), new LiteralText(text[1]), new LiteralText(text[2]), new LiteralText(text[3])));

        event.cancel();
    });
}
