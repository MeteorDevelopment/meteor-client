/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TPAMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAccept = settings.createGroup("Auto Accept");

    private final Setting<String> targetPlayer = sgGeneral.add(new StringSetting.Builder()
        .name("target-player")
        .description("Player to TPA to.")
        .defaultValue("")
        .build()
    );

    private final Setting<Keybind> tpaKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("tpa-key")
        .description("Key to send TPA request.")
        .defaultValue(Keybind.none())
        .action(this::sendTPA)
        .build()
    );

    private final Setting<Boolean> autoAccept = sgAccept.add(new BoolSetting.Builder()
        .name("auto-accept")
        .description("Automatically accept TPA requests from the players below.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> acceptFrom = sgAccept.add(new StringListSetting.Builder()
        .name("accept-from")
        .description("Only these players are auto-accepted.")
        .visible(autoAccept::get)
        .build()
    );

    private final Setting<Boolean> acceptAnyone = sgAccept.add(new BoolSetting.Builder()
        .name("accept-anyone")
        .description("Accept requests from anyone. Dangerous: enemies can teleport straight to you.")
        .defaultValue(false)
        .visible(autoAccept::get)
        .build()
    );

    // The exact wording depends on the server's TPA plugin, so match the common phrasings.
    private static final Pattern[] REQUEST_PATTERNS = {
        Pattern.compile("(?i)\\b([A-Za-z0-9_]{3,16}) has requested to teleport to you"),
        Pattern.compile("(?i)\\b([A-Za-z0-9_]{3,16}) (?:has )?sent you a (?:teleport|tpa) request"),
        Pattern.compile("(?i)(?:teleport|tpa) request from ([A-Za-z0-9_]{3,16})"),
        Pattern.compile("(?i)\\b([A-Za-z0-9_]{3,16}) wants to teleport to you")
    };

    private final Map<String, Long> lastAccepted = new HashMap<>();

    public TPAMacro() {
        super(Categories.Donut, "tpa-macro", "Quick TPA commands for Donut SMP.");
    }

    private void sendTPA() {
        if (mc.player != null && !targetPlayer.get().isEmpty()) {
            mc.player.connection.sendCommand("tpa " + targetPlayer.get());
        }
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        if (!autoAccept.get() || mc.player == null) return;

        String message = event.getMessage().getString();
        String lower = message.toLowerCase(Locale.ROOT);

        // Our own outgoing confirmation ("Teleport request sent to X") must not trigger an accept.
        if (!lower.contains("teleport") && !lower.contains("tpa")) return;
        if (lower.contains("sent to")) return;

        String name = null;
        for (Pattern pattern : REQUEST_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                name = matcher.group(1);
                break;
            }
        }

        if (name == null) {
            if (acceptAnyone.get() && lower.contains("request")) accept(null);
            return;
        }

        if (name.equalsIgnoreCase(mc.player.getName().getString())) return;
        if (!acceptAnyone.get() && acceptFrom.get().stream().noneMatch(name::equalsIgnoreCase)) return;

        accept(name);
    }

    private void accept(String name) {
        String key = name == null ? "" : name.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        Long last = lastAccepted.get(key);
        if (last != null && now - last < 5000) return;
        lastAccepted.put(key, now);

        mc.player.connection.sendCommand(name == null ? "tpaccept" : "tpaccept " + name);
        info(name == null ? "Accepted TPA request." : "Accepted TPA request from " + name + ".");
    }
}
