package minegame159.meteorclient.modules;

import me.zero.alpine.listener.Listenable;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Module implements Listenable {
    protected static MinecraftClient mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final int color;
    public final List<Setting> settings = new ArrayList<>();
    public final boolean setting;
    private int key = -1;

    private boolean active;
    private boolean visible;

    public Module(Category category, String name, String description, boolean setting, boolean visible) {
        this.category = category;
        this.name = name.toLowerCase();
        title = Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        this.description = description;
        this.setting = setting;
        this.visible = visible;
        color = Color.fromRGBA(Utils.random(180, 255), Utils.random(180, 255), Utils.random(180, 255), 255);
        mc = MinecraftClient.getInstance();
    }

    public Module(Category category, String name, String description, boolean setting) {
        this(category, name, description, setting, true);
    }

    public Module(Category category, String name, String description) {
        this(category, name, description, false);
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle() {
        if (setting) return;

        if (!active) {
            active = true;
            ModuleManager.addActive(this);
            MeteorClient.eventBus.subscribe(this);
            onActivate();
        }
        else {
            active = false;
            ModuleManager.removeActive(this);
            MeteorClient.eventBus.unsubscribe(this);
            onDeactivate();
        }
    }

    public Setting getSetting(String name) {
        for (Setting setting : settings) {
            if (name.equalsIgnoreCase(setting.name)) return setting;
        }

        return null;
    }

    public <T> Setting<T> addSetting(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isActive() {
        return active;
    }

    public String getInfoString() {
        return null;
    }

    public void setKey(int key) {
        if (setting) return;

        this.key = key;
        MeteorClient.eventBus.post(EventStore.moduleBindChangedEvent(this));
    }

    public int getKey() {
        return key;
    }

    public void setVisible(boolean visible) {
        if (setting) return;

        this.visible = visible;
        MeteorClient.eventBus.post(EventStore.moduleVisibilityChangedEvent(this));
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Module && name.equals(((Module) obj).name);
    }
}
