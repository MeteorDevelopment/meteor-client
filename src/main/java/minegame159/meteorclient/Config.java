package minegame159.meteorclient;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.*;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Config extends Savable<Config> {
    public static final Config INSTANCE = new Config();

    private String version = "0.2.0";
    private String prefix = ".";
    public AutoCraft autoCraft = new AutoCraft();

    private Map<WindowType, WindowConfig> windowConfigs = new HashMap<>();
    private Map<Category, Color> categoryColors = new HashMap<>();

    private Config() {
        super(new File(MeteorClient.FOLDER, "config.nbt"));
    }

    public String getVersion() {
        return version;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public String getPrefix() {
        return prefix;
    }

    public WindowConfig getWindowConfig(WindowType type, boolean defaultExpanded) {
        return windowConfigs.computeIfAbsent(type, type1 -> new WindowConfig(defaultExpanded));
    }

    public void setCategoryColor(Category category, Color color) {
        categoryColors.put(category, color);
        save();
    }

    public Color getCategoryColor(Category category) {
        return categoryColors.get(category);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("version", version);
        tag.putString("prefix", prefix);
        tag.put("autoCraft", autoCraft.toTag());
        tag.put("windowConfigs", NbtUtils.mapToTag(windowConfigs));
        tag.put("categoryColors", NbtUtils.mapToTag(categoryColors));

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        prefix = tag.getString("prefix");
        autoCraft.fromTag(tag.getCompound("autoCraft"));
        windowConfigs = NbtUtils.mapFromTag(tag.getCompound("windowConfigs"), WindowType::valueOf, tag1 -> new WindowConfig(false).fromTag((CompoundTag) tag1));
        categoryColors = NbtUtils.mapFromTag(tag.getCompound("categoryColors"), Category::valueOf, tag1 -> new Color().fromTag((CompoundTag) tag1));

        return this;
    }

    public class AutoCraft implements ISerializable<AutoCraft> {
        private boolean craftByOne = true;
        private boolean stopWhenNoIngredients = true;

        private AutoCraft() {}

        public void setCraftByOne(boolean craftByOne) {
            this.craftByOne = craftByOne;
            save();
        }

        public boolean isCraftByOne() {
            return craftByOne;
        }

        public void setStopWhenNoIngredients(boolean stopWhenNoIngredients) {
            this.stopWhenNoIngredients = stopWhenNoIngredients;
            save();
        }

        public boolean isStopWhenNoIngredients() {
            return stopWhenNoIngredients;
        }

        @Override
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();

            tag.putBoolean("craftByOne", craftByOne);
            tag.putBoolean("stopWhenNoIngredients", stopWhenNoIngredients);

            return tag;
        }

        @Override
        public AutoCraft fromTag(CompoundTag tag) {
            craftByOne = tag.getBoolean("craftByOne");
            stopWhenNoIngredients = tag.getBoolean("stopWhenNoIngredients");

            return this;
        }
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
            save();
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            save();
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
}
