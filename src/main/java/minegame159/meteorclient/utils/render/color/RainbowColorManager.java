package minegame159.meteorclient.utils.render.color;

import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;

import java.util.ArrayList;
import java.util.List;

public class RainbowColorManager {
    private static final List<Setting<SettingColor>> colorSettings = new ArrayList<>();
    private static final List<SettingColor> colors = new ArrayList<>();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(onTick);
    }

    public static void addColorSetting(Setting<SettingColor> setting) {
        colorSettings.add(setting);
    }

    public static void addColor(SettingColor color) {
        colors.add(color);
    }

    private static final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        for (Setting<SettingColor> setting : colorSettings) {
            if (setting.module == null || setting.module.isActive()) setting.get().update();
        }

        for (SettingColor color : colors) {
            color.update();
        }

        for (Waypoint waypoint : Waypoints.INSTANCE) {
            waypoint.color.update();
        }
    });
}
