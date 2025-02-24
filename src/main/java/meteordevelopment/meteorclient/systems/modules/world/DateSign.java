package meteordevelopment.meteorclient.systems.modules.world;

import java.text.SimpleDateFormat;
import java.util.Date;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

public class DateSign extends Module {
    public DateSign() {
        super(Categories.World, "date-sign", "Automatically writes the currend date on signs.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> match = sgGeneral.add(new StringSetting.Builder()
            .name("match")
            .description("The text to replace.")
            .defaultValue("@today")
            .build());

    private final Setting<String> format = sgGeneral.add(new StringSetting.Builder()
            .name("format")
            .description("How to format the date.")
            .defaultValue("yyyy/MM/dd HH:mm")
            .onChanged(v -> {
                try {
                    formatter = new SimpleDateFormat(v);
                } catch (Exception e) {
                    formatter = null;
                }
            })
            .build());

    private SimpleDateFormat formatter;

    private String apply(String i) {
        if (formatter == null)
            return i;
        return i.replace(match.get(), formatter.format(new Date()));
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof UpdateSignC2SPacket packet))
            return;

        final var t = packet.getText();
        try {
            event.packet = new UpdateSignC2SPacket(
                    packet.getPos(),
                    packet.isFront(),
                    apply(t[0]),
                    apply(t[1]),
                    apply(t[2]),
                    apply(t[3]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
