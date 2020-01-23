package minegame159.meteorclient.modules.misc;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.server.network.packet.UpdateSignC2SPacket;
import net.minecraft.text.LiteralText;

import java.lang.reflect.Field;

public class AutoSign extends Module {
    private String[] text;

    public AutoSign() {
        super(Category.Misc, "auto-sign", "Automatically writes signs. When enabled first sign's text will be used.");
    }

    @Override
    public void onDeactivate() {
        text = null;
    }

    @SubscribeEvent
    private void onSendPacket(SendPacketEvent e) {
        if (!(e.packet instanceof UpdateSignC2SPacket)) return;

        text = ((UpdateSignC2SPacket) e.packet).getText();
    }

    @SubscribeEvent
    private void onOpenScreen(OpenScreenEvent e) {
        if (!(e.screen instanceof SignEditScreen) || text == null) return;

        try {
            Field field = SignEditScreen.class.getDeclaredField("sign");
            field.setAccessible(true);
            SignBlockEntity sign = (SignBlockEntity) field.get(e.screen);

            mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), new LiteralText(text[0]), new LiteralText(text[1]), new LiteralText(text[2]), new LiteralText(text[3])));
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            ex.printStackTrace();
        }

        e.setCancelled(true);
    }
}
