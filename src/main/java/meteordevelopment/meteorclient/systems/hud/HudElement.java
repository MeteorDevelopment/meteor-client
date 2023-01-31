/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.other.Snapper;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class HudElement implements Snapper.Element, ISerializable<HudElement> {
    public final HudElementInfo<?> info;
    private boolean active;

    public final Settings settings = new Settings();
    public final HudBox box = new HudBox(this);

    public final Keybind keybind = Keybind.none();
    public boolean toggleOnBindRelease = false;
    public boolean chatFeedback = true;

    public boolean autoAnchors = true;
    public int x, y;

    public HudElement(HudElementInfo<?> info) {
        this.info = info;
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        active = !active;
    }

    public void setSize(double width, double height) {
        box.setSize(width, height);
    }

    @Override
    public void setPos(int x, int y) {
        if (autoAnchors) {
            box.setPos(x, y);
            box.xAnchor = XAnchor.Left;
            box.yAnchor = YAnchor.Top;
            box.updateAnchors();
        }
        else {
            box.setPos(box.x + (x - this.x), box.y + (y - this.y));
        }

        updatePos();
    }

    @Override
    public void move(int deltaX, int deltaY) {
        box.move(deltaX, deltaY);
        updatePos();
    }

    public void updatePos() {
        x = box.getRenderX();
        y = box.getRenderY();
    }

    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth(), width, alignment);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return box.width;
    }

    @Override
    public int getHeight() {
        return box.height;
    }

    protected boolean isInEditor() {
        return !Utils.canUpdate() || HudEditorScreen.isOpen();
    }

    public void remove() {
        Hud.get().remove(this);
    }

    public void tick(HudRenderer renderer) {}

    public void render(HudRenderer renderer) {}

    public void onFontChanged() {}

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            ChatUtils.sendMsg(this.hashCode(), Formatting.GRAY, "Toggled (highlight)%s(default) %s(default).", info.title, isActive() ? Formatting.GREEN + "on" : Formatting.RED + "off");
        }
    }

    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(info.title, message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.info(info.title, message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warning(info.title, message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.error(info.title, message, args);
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", info.name);
        tag.putBoolean("active", active);

        tag.put("keybind", keybind.toTag());
        tag.putBoolean("toggleOnKeyRelease", toggleOnBindRelease);
        tag.putBoolean("chatFeedback", chatFeedback);

        tag.put("settings", settings.toTag());
        tag.put("box", box.toTag());

        tag.putBoolean("autoAnchors", autoAnchors);

        return tag;
    }

    @Override
    public HudElement fromTag(NbtCompound tag) {
        settings.reset();

        active = tag.getBoolean("active");

        keybind.fromTag(tag);
        toggleOnBindRelease = tag.getBoolean("toggleOnKeyRelease");
        chatFeedback = tag.getBoolean("chatFeedback");

        settings.fromTag(tag.getCompound("settings"));
        box.fromTag(tag.getCompound("box"));

        autoAnchors = tag.getBoolean("autoAnchors");

        x = box.getRenderX();
        y = box.getRenderY();

        return this;
    }
}
