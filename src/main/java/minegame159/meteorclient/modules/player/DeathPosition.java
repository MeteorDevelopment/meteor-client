package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TookDamageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DeathPosition extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> createWaypoint = sgGeneral.add(new BoolSetting.Builder()
            .name("create-waypoint")
            .description("Creates waypoint when you die.")
            .defaultValue(true)
            .build()
    );

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final WLabel label = new WLabel("No latest death");

    public DeathPosition() {
        super(Category.Player, "death-position", "Sends to your chat where you died.");
    }

    @EventHandler
    private final Listener<TookDamageEvent> onTookDamage = new Listener<>(event -> {
        if (event.entity.getUuid() != null && event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() <= 0) {
            label.setText(String.format("Latest death: %.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));

            String time = dateFormat.format(new Date());
            Chat.info(this, "Died at (highlight)%.0f(default), (highlight)%.0f(default), (highlight)%.0f (default)on (highlight)%s(default).", mc.player.getX(), mc.player.getY(), mc.player.getZ(), time);

            // Create waypoint
            if (createWaypoint.get()) {
                Waypoint waypoint = new Waypoint();
                waypoint.name = "Death " + time;

                waypoint.x = (int) mc.player.getX();
                waypoint.y = (int) mc.player.getY() + 2;
                waypoint.z = (int) mc.player.getZ();
                waypoint.maxVisibleDistance = Integer.MAX_VALUE;

                if (mc.world.getRegistryKey().getValue().getPath().equals("overworld")) waypoint.overworld = true;
                else if (mc.world.getRegistryKey().getValue().getPath().equals("the_nether")) waypoint.nether = true;
                else if (mc.world.getRegistryKey().getValue().getPath().equals("the_end")) waypoint.end = true;

                Waypoints.INSTANCE.add(waypoint);
            }
        }
    });

    @Override
    public WWidget getWidget() {
        return label;
    }
}
