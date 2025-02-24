package meteordevelopment.meteorclient.systems.modules.world;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

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
        super(Categories.World, "date-sign", "Automatically writes the currend date (UTC) on signs.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> match = sgGeneral.add(new StringSetting.Builder()
            .name("match")
            .description("The text to replace.")
            .defaultValue("@NOW")
            .build());

    private final Setting<String> format = sgGeneral.add(new StringSetting.Builder()
            .name("format")
            .description("How to format the date.")
            .defaultValue("yyyy/MM/dd HH:mm")
            .onChanged(v -> {
                try {
                    formatter = DateTimeFormatter.ofPattern(v);
                } catch (Exception e) {
                    info(e.getMessage());
                    formatter = null;
                }
            })
            .build());

    private DateTimeFormatter formatter;

    private String apply(String i) {
        if (formatter == null)
            return i;
        return i.replace(match.get(), Instant.now().atOffset(ZoneOffset.UTC).format(formatter));
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof UpdateSignC2SPacket packet))
            return;

        final var text = packet.getText();
        for (var i = 0; i < text.length; i++)
            text[i] = apply(text[i]);

        if (text.equals(packet.getText()))
            return;

        event.setCancelled(true);
        mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(
                packet.getPos(),
                packet.isFront(),
                text[0], text[1], text[2], text[3]));
    }
}
