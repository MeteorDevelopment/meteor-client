/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MeteorTranslations;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Module implements ISerializable<Module>, Comparable<Module> {
    protected final MinecraftClient mc;

    public final Category category;
    public final String name;
    public final String[] aliases;
    public final Color color;

    public final MeteorAddon addon;
    public final Settings settings;

    private boolean active;

    public boolean serialize = true;
    public boolean runInMainMenu = false;
    public boolean autoSubscribe = true;

    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;
    public boolean favorite = false;

    public Module(Category category, String name, String... aliases) {
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Module '%s' contains invalid characters in its name.".formatted(name));
        }

        this.mc = MinecraftClient.getInstance();
        this.category = category;
        this.name = name;
        this.aliases = aliases;
        this.color = Color.fromHsv(Utils.random(0.0, 360.0), 0.35, 1);
        this.settings = new Settings("module." + name);

        String classname = this.getClass().getName();
        for (MeteorAddon addon : AddonManager.ADDONS) {
            if (classname.startsWith(addon.getPackage())) {
                this.addon = addon;
                return;
            }
        }

        this.addon = null;
    }

    public Module(Category category, String name) {
        this(category, name, new String[0]);
    }

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    public void onActivate() {}
    public void onDeactivate() {}

    public void toggle() {
        if (!active) {
            active = true;
            Modules.get().addActive(this);

            settings.onActivated();

            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) MeteorClient.EVENT_BUS.subscribe(this);
                onActivate();
            }
        }
        else {
            if (runInMainMenu || Utils.canUpdate()) {
                if (autoSubscribe) MeteorClient.EVENT_BUS.unsubscribe(this);
                onDeactivate();
            }

            active = false;
            Modules.get().removeActive(this);
        }
    }

    public void enable() {
        if (!isActive()) toggle();
    }

    public void disable() {
        if (isActive()) toggle();
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Toggled (highlight)%s(default) %s(default).", null /* todo translatable Text */, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
        }
    }

    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(this.getTranslationKey(), message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.infoPrefix(this.getTranslationKey(), message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warningPrefix(this.getTranslationKey(), message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.errorPrefix(this.getTranslationKey(), message, args);
    }

    public boolean isActive() {
        return active;
    }

    public String getInfoString() {
        return null;
    }

    public String getTranslationKey() {
        return "module." + this.name;
    }

    public String getTitle() {
        return MeteorTranslations.translate(this.getTranslationKey());
    }

    public MutableText getTitleText() {
        return MeteorClient.translatable(this.getTranslationKey());
    }

    public String getDescription() {
        return MeteorTranslations.translate(this.getTranslationKey() + ".description");
    }

    public MutableText getDescriptionText() {
        return MeteorClient.translatable(this.getTranslationKey() + ".description");
    }

    @Override
    public NbtCompound toTag() {
        if (!serialize) return null;
        NbtCompound tag = new NbtCompound();

        tag.putString("name", name);
        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.putBoolean("chatFeedback", chatFeedback);
        tag.putBoolean("favorite", favorite);
        tag.put("settings", settings.toTag());
        tag.putBoolean("active", active);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        // General
        keybind.fromTag(tag.getCompoundOrEmpty("keybind"));
        toggleOnBindRelease = tag.getBoolean("toggleOnKeyRelease", false);
        chatFeedback = !tag.contains("chatFeedback") || tag.getBoolean("chatFeedback", false);
        favorite = tag.getBoolean("favorite", false);

        // Settings
        NbtElement settingsTag = tag.get("settings");
        if (settingsTag instanceof NbtCompound) settings.fromTag((NbtCompound) settingsTag);

        boolean active = tag.getBoolean("active", false);
        if (active != isActive()) toggle();

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Module module = (Module) o;
        return Objects.equals(name, module.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@NotNull Module o) {
        return name.compareTo(o.name);
    }
}
