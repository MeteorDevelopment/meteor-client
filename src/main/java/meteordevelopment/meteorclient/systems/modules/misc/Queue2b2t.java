package meteordevelopment.meteorclient.systems.modules.misc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Queue2b2t extends Module {
    private static final String match = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPosition in queue: ";
    private final HttpClient http = HttpClient.newHttpClient();

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> webhook = sg.add(new StringSetting.Builder()
            .name("webhook")
            .description("Discord webhook to send message to.")
            .build());

    private final Setting<Integer> position = sg.add(new IntSetting.Builder()
            .name("position")
            .description("Position at which to start sending messages.")
            .defaultValue(5)
            .min(1)
            .noSlider()
            .build());

    private final Setting<Boolean> once = sg.add(new BoolSetting.Builder()
            .name("once")
            .description("Only send one message, when crossed position.")
            .defaultValue(false)
            .build());

    private int last;

    public Queue2b2t() {
        super(Categories.Misc, "queue-2b2t", "Alert position in the 2b2t queue.");
    }

    @Override
    public void onActivate() {
        this.last = 0;
    }

    private void alert(final String s) throws Exception {
        final var data = "{\"content\": \"" + s.replace("\"", "\\\"") + "\"}";
        final var request = HttpRequest.newBuilder(URI.create(webhook.get()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    @EventHandler(priority = 1)
    private void onMessageReceive(final ReceiveMessageEvent event) {
        if (!mc.player.isSpectator())
            return;
        if (mc.getCurrentServerEntry() == null || !mc.getCurrentServerEntry().address.equals("2b2t.org"))
            return;

        final var message = event.getMessage().getString();
        if (!message.startsWith(match))
            return;
        final var s = message.substring(match.length()).split("\n")[0];
        try {
            final var i = Integer.parseInt(s);
            if (i == last)
                return;
            if (i > last || (i <= position.get() && (!once.get() || last > position.get())))
                alert(mc.player.getName().getString() + " " + i);
            last = i;
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
