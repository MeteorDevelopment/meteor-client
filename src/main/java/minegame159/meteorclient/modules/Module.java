package minegame159.meteorclient.modules;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class Module {
    protected static MinecraftClient mc;

    public final Category category;
    public final String name;
    public final String title;
    public final String description;
    public final int color;
    public final Setting[] settings;
    public int key = -1;

    private boolean active;

    public Module(Category category, String name, String description, Setting... settings) {
        this.category = category;
        this.name = name.toLowerCase();
        title = Arrays.stream(name.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        this.description = description;
        color = Color.fromRGBA(Utils.random(180, 255), Utils.random(180, 255), Utils.random(180, 255), 255);
        this.settings = settings == null ? new Setting[0] : settings;
        mc = MinecraftClient.getInstance();
    }

    public void onActivate() {}
    public void onDeactivate() {}
    public void onSettingChanges() {}

    public void toggle() {
        if (!active) {
            active = true;
            ModuleManager.addActive(this);
            MeteorClient.eventBus.register(this);
            onActivate();
        }
        else {
            active = false;
            ModuleManager.removeActive(this);
            MeteorClient.eventBus.unregister(this);
            onDeactivate();
        }
    }

    public Setting getSetting(String name) {
        for (int i = 0; i < settings.length; i++) {
            Setting setting = settings[i];
            if (name.equalsIgnoreCase(setting.name)) return setting;
        }

        return null;
    }

    public boolean isActive() {
        return active;
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Module && name.equals(((Module) obj).name);
    }
}
