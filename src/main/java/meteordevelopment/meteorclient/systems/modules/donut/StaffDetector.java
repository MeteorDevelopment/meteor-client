package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.elements.EmberNotificationsHud;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class StaffDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAlerts = settings.createGroup("Alerts");

    private final Setting<List<String>> staffNames = sgGeneral.add(new StringListSetting.Builder()
        .name("staff-names")
        .description("Usernames that always count as staff.")
        .build()
    );

    private final Setting<Boolean> checkRanks = sgGeneral.add(new BoolSetting.Builder()
        .name("check-ranks")
        .description("Treat players whose tab list rank contains one of the rank words as staff.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> rankWords = sgGeneral.add(new StringListSetting.Builder()
        .name("rank-words")
        .description("Rank tags that mark staff, matched as whole words.")
        .defaultValue("Owner", "Admin", "Manager", "Mod", "Moderator", "Helper", "Staff", "Developer")
        .visible(checkRanks::get)
        .build()
    );

    private final Setting<Boolean> alertNearby = sgAlerts.add(new BoolSetting.Builder()
        .name("alert-nearby")
        .description("Alert when a staff member comes into render distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> alertLeave = sgAlerts.add(new BoolSetting.Builder()
        .name("alert-leave")
        .description("Alert when a staff member leaves the server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chat = sgAlerts.add(new BoolSetting.Builder()
        .name("chat")
        .description("Post alerts in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notification = sgAlerts.add(new BoolSetting.Builder()
        .name("notification")
        .description("Show alerts in the Ember notifications widget.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sound = sgAlerts.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a sound on alerts.")
        .defaultValue(true)
        .build()
    );

    /** Staff currently online, name to the rank that matched. */
    private final Map<String, String> online = new HashMap<>();
    private final Set<String> nearby = new HashSet<>();
    private boolean primed;
    private int timer;

    public StaffDetector() {
        super(Categories.Donut, "staff-detector", "Alerts you when staff are online or near you.");
    }

    @Override
    public void onActivate() {
        online.clear();
        nearby.clear();
        primed = false;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getConnection() == null || mc.level == null || mc.player == null) return;
        if (timer-- > 0) return;
        timer = 20;

        String self = mc.player.getName().getString();
        Map<String, String> now = new HashMap<>();

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name == null || name.equalsIgnoreCase(self)) continue;

            String rank = staffRank(info, name);
            if (rank != null) now.put(name, rank);
        }

        if (!primed) {
            // First scan after joining: one summary instead of a "joined" alert per staff member.
            primed = true;
            if (!now.isEmpty()) alert("Staff online", String.join(", ", now.keySet()));
        } else {
            for (Map.Entry<String, String> entry : now.entrySet()) {
                if (!online.containsKey(entry.getKey())) alert(entry.getKey() + " joined", entry.getValue());
            }
            if (alertLeave.get()) {
                for (Map.Entry<String, String> entry : online.entrySet()) {
                    if (!now.containsKey(entry.getKey())) alert(entry.getKey() + " left", entry.getValue());
                }
            }
        }

        online.clear();
        online.putAll(now);

        if (alertNearby.get()) {
            Set<String> seen = new HashSet<>();

            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;

                String name = player.getName().getString();
                if (!online.containsKey(name)) continue;

                seen.add(name);
                if (nearby.add(name)) {
                    alert(name + " is near you", String.format("%.0f blocks away", mc.player.distanceTo(player)));
                }
            }

            nearby.retainAll(seen);
        }
    }

    private String staffRank(PlayerInfo info, String name) {
        for (String listed : staffNames.get()) {
            if (listed.trim().equalsIgnoreCase(name)) return "Listed staff";
        }

        if (!checkRanks.get()) return null;

        StringBuilder text = new StringBuilder();
        if (info.getTabListDisplayName() != null) text.append(info.getTabListDisplayName().getString()).append(' ');

        PlayerTeam team = info.getTeam();
        if (team != null) {
            text.append(team.getPlayerPrefix().getString()).append(' ').append(team.getPlayerSuffix().getString());
        }

        // A username can itself contain a rank word ("mod_steve"), so only the decoration around it is checked.
        String decoration = text.toString().replace(name, " ");

        for (String word : rankWords.get()) {
            String trimmed = word.trim();
            if (trimmed.isEmpty()) continue;

            Pattern pattern = Pattern.compile("(?i)(?<![A-Za-z])" + Pattern.quote(trimmed) + "(?![A-Za-z])");
            if (pattern.matcher(decoration).find()) return trimmed;
        }

        return null;
    }

    private void alert(String title, String subtitle) {
        if (chat.get()) info("%s (%s)", title, subtitle);
        if (notification.get()) EmberNotificationsHud.push(title, subtitle, EmberNotificationsHud.BAD);
        if (sound.get() && mc.player != null) mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
    }
}
