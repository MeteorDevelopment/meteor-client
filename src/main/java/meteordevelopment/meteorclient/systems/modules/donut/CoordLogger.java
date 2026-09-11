/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoordLogger extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> logToChat = sgGeneral.add(new BoolSetting.Builder()
        .name("log-to-chat")
        .description("Log found coordinates to chat.")
        .defaultValue(true)
        .build()
    );

    private static final Pattern COORD_PATTERN = Pattern.compile("(-?\\d+)[,\\s]+(-?\\d+)[,\\s]+(-?\\d+)");

    public CoordLogger() {
        super(Categories.Donut, "coord-logger", "Logs coordinates mentioned in chat.");
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        Matcher matcher = COORD_PATTERN.matcher(msg);

        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));

            if (logToChat.get()) {
                info("Coords found: %d, %d, %d", x, y, z);
            }
        }
    }
}
