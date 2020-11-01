package minegame159.meteorclient.modules;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Formatting;

public abstract class ToggleModule extends Module {
    private boolean active;
    private boolean visible = true;

    public ToggleModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle(boolean onActivateDeactivate) {
        if (!active) {
            active = true;
            ModuleManager.INSTANCE.addActive(this);

            for (SettingGroup sg : settings) {
                for (Setting setting : sg) {
                    if (setting.onModuleActivated != null) setting.onModuleActivated.accept(setting);
                }
            }

            if (onActivateDeactivate) {
                MeteorClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
        else {
            active = false;
            ModuleManager.INSTANCE.removeActive(this);

            if (onActivateDeactivate) {
                MeteorClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }
        }
    }
    public void toggle() {
        toggle(true);
    }

    @Override
    public void doAction(boolean onActivateDeactivate) {
        toggle(onActivateDeactivate);
    }

    public String getInfoString() {
        return null;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = super.toTag();

        tag.putBoolean("active", active);
        tag.putBoolean("visible", visible);

        return tag;
    }

    @Override
    public ToggleModule fromTag(CompoundTag tag) {
        super.fromTag(tag);

        boolean active = tag.getBoolean("active");
        if (active != isActive()) toggle(Utils.canUpdate());
        setVisible(tag.getBoolean("visible"));

        return this;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        MeteorClient.EVENT_BUS.post(EventStore.moduleVisibilityChangedEvent(this));
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isActive() {
        return active;
    }

    public void sendToggledMsg() {
        if (Config.INSTANCE.chatCommandsInfo) Chat.info("Toggled (highlight)%s(default) %s(default).", title, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
    }
}
