package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

public class ScriptSign extends Module {
    public ScriptSign() {
        super(Categories.World, "script-sign", "Enables the use of Starscript in signs.");
    }

    // TODO: change AutoSign behavior to re-eval

    @EventHandler
    private void onSendPacket(final PacketEvent.Send event) {
        if (!(event.packet instanceof final UpdateSignC2SPacket packet))
            return;

        final var text = packet.getText();
        for (var i = 0; i < text.length; i++)
            text[i] = MeteorStarscript.run(MeteorStarscript.compile(text[i]));

        if (text.equals(packet.getText()))
            return;

        event.setCancelled(true);
        mc.getNetworkHandler().sendPacket(
                new UpdateSignC2SPacket(
                        packet.getPos(),
                        packet.isFront(),
                        text[0], text[1], text[2], text[3]));
    }
}
