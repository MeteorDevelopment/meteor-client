package minegame159.meteorclient.gui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class GuiConfig implements ISerializable<GuiConfig> {
    public static GuiConfig INSTANCE;

    public Color text = new Color(255, 255, 255);
    public Color windowHeaderText = new Color(255, 255, 255);
    public Color loggedInText = new Color(45, 225, 45);

    public Color background = new Color(20, 20, 20, 200);
    public Color backgroundHovered = new Color(30, 30, 30, 200);
    public Color backgroundPressed = new Color(40, 40, 40, 200);

    public Color outline = new Color(0, 0, 0, 225);
    public Color outlineHovered = new Color(10, 10, 10, 225);
    public Color outlinePressed = new Color(20, 20, 20, 225);

    public Color checkbox = new Color(45, 225, 45);
    public Color checkboxPressed = new Color(70, 225, 70);

    public Color separator = new Color(10, 10, 10, 225);

    public Color plus = new Color(45, 225, 45);
    public Color plusPressed = new Color(70, 225, 70);

    public Color minus = new Color(225, 45, 45);
    public Color minusPressed = new Color(225, 70, 70);

    public Color accent = new Color(0, 255, 180);

    public Color moduleBackground = new Color(50, 50, 50);

    private Map<WindowType, WindowConfig> windowConfigs = new HashMap<>();

    public GuiConfig() {
        INSTANCE = this;
    }

    public WindowConfig getWindowConfig(WindowType type, boolean defaultExpanded) {
        return windowConfigs.computeIfAbsent(type, type1 -> new WindowConfig(defaultExpanded));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.put("text", text.toTag());
        tag.put("windowHeaderText", text.toTag());
        tag.put("loggedInText", loggedInText.toTag());

        tag.put("background", background.toTag());
        tag.put("backgroundHovered", backgroundHovered.toTag());
        tag.put("backgroundPressed", backgroundPressed.toTag());

        tag.put("outline", outline.toTag());
        tag.put("outlineHovered", outlineHovered.toTag());
        tag.put("outlinePressed", outlinePressed.toTag());

        tag.put("checkbox", checkbox.toTag());
        tag.put("checkboxPressed", checkboxPressed.toTag());

        tag.put("separator", separator.toTag());

        tag.put("plus", plus.toTag());
        tag.put("plusPressed", plusPressed.toTag());

        tag.put("minus", minus.toTag());
        tag.put("minusPressed", minusPressed.toTag());

        tag.put("accent", accent.toTag());

        tag.put("moduleBackground", moduleBackground.toTag());

        tag.put("windowConfigs", NbtUtils.mapToTag(windowConfigs));

        return tag;
    }

    @Override
    public GuiConfig fromTag(CompoundTag tag) {
        read(tag, "text", text);
        read(tag, "windowHeaderText", windowHeaderText);
        read(tag, "loggedInText", loggedInText);

        read(tag, "background", background);
        read(tag, "backgroundHovered", backgroundHovered);
        read(tag, "backgroundPressed", backgroundPressed);

        read(tag, "outline", outline);
        read(tag, "outlineHovered", outlineHovered);
        read(tag, "outlinePressed", outlinePressed);

        read(tag, "checkbox", checkbox);
        read(tag, "checkboxPressed", checkboxPressed);

        read(tag, "separator", separator);

        read(tag, "plus", plus);
        read(tag, "plusPressed", plusPressed);

        read(tag, "minus", minus);
        read(tag, "minusPressed", minusPressed);

        read(tag, "accent", accent);

        read(tag, "moduleBackground", moduleBackground);

        windowConfigs = NbtUtils.mapFromTag(tag.getCompound("windowConfigs"), WindowType::valueOf, tag1 -> new WindowConfig(false).fromTag((CompoundTag) tag1));

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

    public class WindowConfig implements ISerializable<WindowConfig> {
        private Vector2 pos = new Vector2(-1, -1);
        private boolean expanded;

        private WindowConfig(boolean expanded) {
            this.expanded = expanded;
        }

        public double getX() {
            return pos.x;
        }

        public double getY() {
            return pos.y;
        }

        public void setPos(double x, double y) {
            this.pos.set(x, y);
            Config.INSTANCE.save();
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
