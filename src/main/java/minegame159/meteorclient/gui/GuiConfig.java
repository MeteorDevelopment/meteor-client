/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.utils.*;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class GuiConfig implements ISerializable<GuiConfig> {
    public static GuiConfig INSTANCE;

    public double guiScale = 1;
    public double scrollSensitivity = 1;

    public AlignmentX moduleNameAlignment = AlignmentX.Center;

    public Color text = new Color(255, 255, 255);
    public Color windowHeaderText = new Color(255, 255, 255);
    public Color loggedInText = new Color(45, 225, 45);
    public Color accountTypeText = new Color(150, 150, 150);

    public Color background = new Color(20, 20, 20, 200);
    public Color backgroundHovered = new Color(30, 30, 30, 200);
    public Color backgroundPressed = new Color(40, 40, 40, 200);

    public Color scrollbar = new Color(80, 80, 80, 200);
    public Color scrollbarHovered = new Color(90, 90, 90, 200);
    public Color scrollbarPressed = new Color(100, 100, 100, 200);

    public Color outline = new Color(0, 0, 0, 225);
    public Color outlineHovered = new Color(10, 10, 10, 225);
    public Color outlinePressed = new Color(20, 20, 20, 225);

    public Color checkbox = new Color(45, 225, 45);
    public Color checkboxPressed = new Color(70, 225, 70);

    public Color separator = new Color(200, 200, 200, 225);

    public Color plus = new Color(45, 225, 45);
    public Color plusHovered = new Color(60, 225, 60);
    public Color plusPressed = new Color(75, 225, 75);

    public Color minus = new Color(225, 45, 45);
    public Color minusHovered = new Color(225, 60, 60);
    public Color minusPressed = new Color(225, 75, 75);

    public Color accent = new Color(135, 0, 255);

    public Color moduleBackground = new Color(50, 50, 50);

    public Color reset = new Color(50, 50, 50);
    public Color resetHovered = new Color(60, 60, 60);
    public Color resetPressed = new Color(70, 70, 70);

    public Color sliderLeft = new Color(0, 150, 80);
    public Color sliderRight = new Color(50, 50, 50);

    public Color sliderHandle = new Color(0, 255, 180);
    public Color sliderHandleHovered = new Color(0, 240, 165);
    public Color sliderHandlePressed = new Color(0, 225, 150);

    public Color colorEditHandle = new Color(70, 70, 70);
    public Color colorEditHandleHovered = new Color(80, 80, 80);
    public Color colorEditHandlePressed = new Color(90, 90, 90);

    public Color edit = new Color(50, 50, 50);
    public Color editHovered = new Color(60, 60, 60);
    public Color editPressed = new Color(70, 70, 70);

    private Map<WindowType, WindowConfig> windowConfigs = new HashMap<>();

    public GuiConfig() {
        INSTANCE = this;
    }

    public WindowConfig getWindowConfig(WindowType type) {
        return windowConfigs.computeIfAbsent(type, type1 -> new WindowConfig());
    }

    public void clearWindowConfigs() {
        windowConfigs.clear();
        Config.INSTANCE.save();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putDouble("guiScale", guiScale);
        tag.putDouble("scrollSensitivity", scrollSensitivity);

        tag.putString("moduleNameAlignment", moduleNameAlignment.name());

        tag.put("text", text.toTag());
        tag.put("windowHeaderText", text.toTag());
        tag.put("loggedInText", loggedInText.toTag());
        tag.put("accountTypeText", accountTypeText.toTag());

        tag.put("background", background.toTag());
        tag.put("backgroundHovered", backgroundHovered.toTag());
        tag.put("backgroundPressed", backgroundPressed.toTag());

        tag.put("scrollbar", scrollbar.toTag());
        tag.put("scrollbarHovered", scrollbarHovered.toTag());
        tag.put("scrollbarPressed", scrollbarPressed.toTag());

        tag.put("outline", outline.toTag());
        tag.put("outlineHovered", outlineHovered.toTag());
        tag.put("outlinePressed", outlinePressed.toTag());

        tag.put("checkbox", checkbox.toTag());
        tag.put("checkboxPressed", checkboxPressed.toTag());

        tag.put("separator", separator.toTag());

        tag.put("plus", plus.toTag());
        tag.put("plusHovered", plusHovered.toTag());
        tag.put("plusPressed", plusPressed.toTag());

        tag.put("minus", minus.toTag());
        tag.put("minusHovered", minusHovered.toTag());
        tag.put("minusPressed", minusPressed.toTag());

        tag.put("accent", accent.toTag());

        tag.put("moduleBackground", moduleBackground.toTag());

        tag.put("reset", reset.toTag());
        tag.put("resetHovered", resetHovered.toTag());
        tag.put("resetPressed", resetPressed.toTag());

        tag.put("sliderLeft", sliderLeft.toTag());
        tag.put("sliderRight", sliderRight.toTag());

        tag.put("sliderHandle", sliderHandle.toTag());
        tag.put("sliderHandleHovered", sliderHandleHovered.toTag());
        tag.put("sliderHandlePressed", sliderHandlePressed.toTag());

        tag.put("colorEditHandle", colorEditHandle.toTag());
        tag.put("colorEditHandleHovered", colorEditHandleHovered.toTag());
        tag.put("colorEditHandlePressed", colorEditHandlePressed.toTag());

        tag.put("edit", edit.toTag());
        tag.put("editHovered", editHovered.toTag());
        tag.put("editPressed", editPressed.toTag());

        tag.put("windowConfigs", NbtUtils.mapToTag(windowConfigs));

        return tag;
    }

    @Override
    public GuiConfig fromTag(CompoundTag tag) {
        if (tag.contains("guiScale")) guiScale = tag.getDouble("guiScale");
        if (tag.contains("scrollSensitivity")) scrollSensitivity = tag.getDouble("scrollSensitivity");

        if (tag.contains("moduleNameAlignment")) moduleNameAlignment = AlignmentX.valueOf(tag.getString("moduleNameAlignment"));

        read(tag, "text", text);
        read(tag, "windowHeaderText", windowHeaderText);
        read(tag, "loggedInText", loggedInText);
        read(tag, "accountTypeText", accountTypeText);

        read(tag, "background", background);
        read(tag, "backgroundHovered", backgroundHovered);
        read(tag, "backgroundPressed", backgroundPressed);

        read(tag, "scrollbar", scrollbar);
        read(tag, "scrollbarHovered", scrollbarHovered);
        read(tag, "scrollbarPressed", scrollbarPressed);

        read(tag, "outline", outline);
        read(tag, "outlineHovered", outlineHovered);
        read(tag, "outlinePressed", outlinePressed);

        read(tag, "checkbox", checkbox);
        read(tag, "checkboxPressed", checkboxPressed);

        read(tag, "separator", separator);

        read(tag, "plus", plus);
        read(tag, "plusHovered", plusHovered);
        read(tag, "plusPressed", plusPressed);

        read(tag, "minus", minus);
        read(tag, "minusHovered", minusHovered);
        read(tag, "minusPressed", minusPressed);

        read(tag, "accent", accent);

        read(tag, "reset", reset);
        read(tag, "resetHovered", resetHovered);
        read(tag, "resetPressed", resetPressed);

        read(tag, "moduleBackground", moduleBackground);

        read(tag, "sliderLeft", sliderLeft);
        read(tag, "sliderRight", sliderRight);

        read(tag, "sliderHandle", sliderHandle);
        read(tag, "sliderHandleHovered", sliderHandleHovered);
        read(tag, "sliderHandlePressed", sliderHandlePressed);

        read(tag, "colorEditHandle", colorEditHandle);
        read(tag, "colorEditHandleHovered", colorEditHandleHovered);
        read(tag, "colorEditHandlePressed", colorEditHandlePressed);

        read(tag, "edit", edit);
        read(tag, "editHovered", editHovered);
        read(tag, "editPressed", editPressed);

        windowConfigs = NbtUtils.mapFromTag(tag.getCompound("windowConfigs"), WindowType::valueOf, tag1 -> new WindowConfig().fromTag((CompoundTag) tag1));

        return this;
    }

    private void read(CompoundTag tag, String name, ISerializable<?> serializable) {
        if (tag.contains(name)) serializable.fromTag(tag.getCompound(name));
    }

    public enum WindowType {
        Combat,
        Player,
        Movement,
        Render,
        Misc,
        Setting,
        Profiles,
        Search
    }

    public static class WindowConfig implements ISerializable<WindowConfig> {
        private final Vector2 pos = new Vector2(-1, -1);
        private boolean expanded;

        public double getX() {
            return pos.x;
        }

        public double getY() {
            return pos.y;
        }

        public void setPos(double x, double y) {
            this.pos.set(x, y);
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            Config.INSTANCE.save();
        }

        @Override
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();

            tag.put("pos", pos.toTag());
            tag.putBoolean("expanded", expanded);

            return tag;
        }

        @Override
        public WindowConfig fromTag(CompoundTag tag) {
            pos.fromTag(tag.getCompound("pos"));
            expanded = tag.getBoolean("expanded");

            return this;
        }
    }
}
