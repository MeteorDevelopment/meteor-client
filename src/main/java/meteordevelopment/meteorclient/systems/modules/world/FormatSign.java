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

// TODO: StarcriptSign or something
public class FormatSign extends Module {
    private final SettingGroup sgPlayer = settings.createGroup("Player");
    private final SettingGroup sgDate = settings.createGroup("Date");

    private final Setting<String> playerMatch = sgPlayer.add(new StringSetting.Builder()
            .name("match")
            .description("The text to replace.")
            .defaultValue("{me}")
            .build());


    private final Setting<String> dateMatch = sgDate.add(new StringSetting.Builder()
            .name("match")
            .description("The text to replace.")
            .defaultValue("{now}")
            .build());

    private final Setting<String> dateFormat = sgDate.add(new StringSetting.Builder()
            .name("format")
            .description("How to format the date.")
            .defaultValue("yyyy/MM/dd HH:mm")
            .onChanged(v -> {
                try {
                    dateFormatter = DateTimeFormatter.ofPattern(v);
                } catch (final Exception e) {
                    info(e.getMessage());
                    dateFormatter = null;
                }
            })
            .build());

    private DateTimeFormatter dateFormatter;

    public FormatSign() {
        super(Categories.World, "format-sign", "Automatically replaces text on signs.");
    }

    private String apply(String i) {
        i = i.replace(playerMatch.get(), mc.player.getName().getString());
        if (dateFormatter == null)
            return i;
        return i.replace(dateMatch.get(), Instant.now().atOffset(ZoneOffset.UTC).format(dateFormatter));
    }

    @EventHandler
    private void onSendPacket(final PacketEvent.Send event) {
        if (!(event.packet instanceof final UpdateSignC2SPacket packet))
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
