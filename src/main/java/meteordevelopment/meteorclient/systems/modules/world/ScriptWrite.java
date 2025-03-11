package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

// TODO: add some visual feedback while editing
public class ScriptWrite extends Module {
    private static String eval(final String s) {
        return MeteorStarscript.run(MeteorStarscript.compile(s));
    }

    private final SettingGroup sgSign = settings.createGroup("Sign");

    private final Setting<Boolean> signSigns = sgSign.add(new BoolSetting.Builder()
            .name("sign")
            .description("Sign signs by setting line 3 to \"{player}\" and 4 to \"{time}\".")
            .defaultValue(true)
            .build());

    private final SettingGroup sgBook = settings.createGroup("Book");

    private final Setting<Boolean> onSign = sgBook.add(new BoolSetting.Builder()
            .name("on-sign")
            .description("Evaluate book content only when signing it.")
            .defaultValue(true)
            .build());

    public ScriptWrite() {
        super(Categories.World, "script-write", "Enables the use of Starscript in signs and books.");
    }

    @EventHandler
    private void onSendPacket(final PacketEvent.Send event) {
        switch (event.packet) {
            case final BookUpdateC2SPacket p:
                if (onSign.get() && !p.title().isPresent())
                    break;

                final var pages = p.pages()
                        .stream()
                        .map(ScriptWrite::eval)
                        .toList();

                if (pages.equals(p.pages()))
                    return;

                event.setCancelled(true);
                mc.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(p.slot(), pages, p.title()));
                break;
            case final UpdateSignC2SPacket p:
                final var text = p.getText();
                if (signSigns.get()) {
                    text[2] = "{player}";
                    text[3] = "{time}";
                }

                for (var i = 0; i < text.length; i++)
                    text[i] = eval(text[i]);

                if (text.equals(p.getText()))
                    return;

                event.setCancelled(true);
                mc.getNetworkHandler().sendPacket(
                        new UpdateSignC2SPacket(
                                p.getPos(),
                                p.isFront(),
                                text[0], text[1], text[2], text[3]));
                break;
            default:
        }
    }
}
