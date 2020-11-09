package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.ModuleListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModuleListSetting extends Setting<List<ToggleModule>> {
    public ModuleListSetting(String name, String description, List<ToggleModule> defaultValue, Consumer<List<ToggleModule>> onChanged, Consumer<Setting<List<ToggleModule>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new ModuleListSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected List<ToggleModule> parseImpl(String str) {
        String[] values = str.split(",");
        List<ToggleModule> modules = new ArrayList<>(1);

        try {
            for (String value : values) {
                Module module = ModuleManager.INSTANCE.get(value.trim());
                if (module instanceof ToggleModule) modules.add((ToggleModule) module);
            }
        } catch (Exception ignored) {}

        return modules;
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<ToggleModule> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)module name (default)(kill-aura, speed, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        ListTag modulesTag = new ListTag();
        for (ToggleModule module : get()) modulesTag.add(StringTag.of(module.name));
        tag.put("modules", modulesTag);

        return tag;
    }

    @Override
    public List<ToggleModule> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("modules", 8);
        for (Tag tagI : valueTag) {
            Module module = ModuleManager.INSTANCE.get(tagI.asString());
            if (module instanceof ToggleModule) get().add((ToggleModule) module);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<ToggleModule> defaultValue;
        private Consumer<List<ToggleModule>> onChanged;
        private Consumer<Setting<List<ToggleModule>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<ToggleModule> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<ToggleModule>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<ToggleModule>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public ModuleListSetting build() {
            return new ModuleListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
