/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.utils.misc.ISerializable;
import minegame159.meteorclient.utils.misc.Vector2;
import minegame159.meteorclient.utils.render.AlignmentX;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class GuiConfig implements ISerializable<GuiConfig> {
    public double guiScale = 1;
    public double scrollSensitivity = 1;

    public AlignmentX moduleNameAlignment = AlignmentX.Center;
    public double moduleNameAlignmentPadding = 7;

    public SettingColor text = createColor(255, 255, 255, 255);
    public SettingColor windowHeaderText = createColor(255, 255, 255, 255);
    public SettingColor loggedInText = createColor(45, 225, 45, 255);
    public SettingColor accountTypeText = createColor(150, 150, 150, 255);

    public SettingColor background = createColor(20, 20, 20, 200);
    public SettingColor backgroundHovered = createColor(30, 30, 30, 200);
    public SettingColor backgroundPressed = createColor(40, 40, 40, 200);

    public SettingColor scrollbar = createColor(80, 80, 80, 200);
    public SettingColor scrollbarHovered = createColor(90, 90, 90, 200);
    public SettingColor scrollbarPressed = createColor(100, 100, 100, 200);

    public SettingColor outline = createColor(0, 0, 0, 225);
    public SettingColor outlineHovered = createColor(10, 10, 10, 225);
    public SettingColor outlinePressed = createColor(20, 20, 20, 225);

    public SettingColor checkbox = createColor(45, 225, 45, 255);
    public SettingColor checkboxPressed = createColor(70, 225, 70, 255);

    public SettingColor separator = createColor(200, 200, 200, 225);

    public SettingColor plus = createColor(45, 225, 45, 255);
    public SettingColor plusHovered = createColor(60, 225, 60, 255);
    public SettingColor plusPressed = createColor(75, 225, 75, 255);

    public SettingColor minus = createColor(225, 45, 45, 255);
    public SettingColor minusHovered = createColor(225, 60, 60, 255);
    public SettingColor minusPressed = createColor(225, 75, 75, 255);

    public SettingColor accent = createColor(135, 0, 255, 255);

    public SettingColor moduleBackground = createColor(50, 50, 50, 255);

    public SettingColor reset = createColor(50, 50, 50, 255);
    public SettingColor resetHovered = createColor(60, 60, 60, 255);
    public SettingColor resetPressed = createColor(70, 70, 70, 255);

    public SettingColor sliderLeft = createColor(0, 150, 80, 255);
    public SettingColor sliderRight = createColor(50, 50, 50, 255);

    public SettingColor sliderHandle = createColor(0, 255, 180, 255);
    public SettingColor sliderHandleHovered = createColor(0, 240, 165, 255);
    public SettingColor sliderHandlePressed = createColor(0, 225, 150, 255);

    public SettingColor colorEditHandle = createColor(70, 70, 70, 255);
    public SettingColor colorEditHandleHovered = createColor(80, 80, 80, 255);
    public SettingColor colorEditHandlePressed = createColor(90, 90, 90, 255);

    public SettingColor edit = createColor(50, 50, 50, 255);
    public SettingColor editHovered = createColor(60, 60, 60, 255);
    public SettingColor editPressed = createColor(70, 70, 70, 255);

    public boolean expandListSettingScreen = true;
    public boolean collapseListSettingScreen = true;
    public int countListSettingScreen = 20;

    private final Map<WindowType, WindowConfig> windowConfigs = new HashMap<>();

    public static GuiConfig get() {
        return Config.get().guiConfig;
    }
    
    private SettingColor createColor(int r, int g, int b, int a) {
        SettingColor color = new SettingColor(r, g, b, a);
        RainbowColors.add(color);
        return color;
    }

    public WindowConfig getWindowConfig(WindowType type) {
        return windowConfigs.computeIfAbsent(type, type1 -> new WindowConfig());
    }

    public void clearWindowConfigs() {
        windowConfigs.clear();

        for (Category category : Modules.loopCategories()) {
            category.windowConfig = new WindowConfig();
        }

        Config.get().save();
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

        ListTag windowConfigsTag = new ListTag();
        for (WindowType type : windowConfigs.keySet()) writeWindowConfig(windowConfigsTag, type, windowConfigs.get(type), null);
        for (Category category : Modules.loopCategories()) writeWindowConfig(windowConfigsTag, WindowType.Category, category.windowConfig, category);
        tag.put("windowConfigs", windowConfigsTag);

        return tag;
    }

    private void writeWindowConfig(ListTag listTag, WindowType type, WindowConfig config, Category category) {
        CompoundTag tag = config.toTag();

        tag.putString("type", type.name());
        if (category != null) tag.putInt("id", category.hashCode());

        listTag.add(tag);
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

        Tag windowConfigsTag = tag.get("windowConfigs");
        windowConfigs.clear();

        if (windowConfigsTag instanceof ListTag) {
            for (Tag t : (ListTag) windowConfigsTag) {
                CompoundTag windowConfigTag = ((CompoundTag) t);

                WindowType type = WindowType.valueOf(windowConfigTag.getString("type"));
                if (type == WindowType.Category) {
                    Category category = Modules.getCategoryByHash(windowConfigTag.getInt("id"));
                    if (category != null) category.windowConfig.fromTag(windowConfigTag);
                }
                else {
                    windowConfigs.put(type, new WindowConfig().fromTag(windowConfigTag));
                }
            }
        }

        return this;
    }

    private void read(CompoundTag tag, String name, ISerializable<?> serializable) {
        if (tag.contains(name)) serializable.fromTag(tag.getCompound(name));
    }

    public enum WindowType {
        Category,
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
            Config.get().save();
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
