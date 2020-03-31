package minegame159.meteorclient;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.*;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Config extends Savable<Config> {
    public static final Config INSTANCE = new Config();

    private String version = "0.1.6";
    private String prefix = ".";
    public AutoCraft autoCraft = new AutoCraft();

    private Map<Category, Vector2> guiPositions = new HashMap<>();
    private Map<Category, Color> categoryColors = new HashMap<>();

    private Config() {
        super(new File(MeteorClient.FOLDER, "config.nbt"));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setGuiPosition(Category category, Vector2 pos) {
        guiPositions.put(category, pos);
        save();
    }

    public Vector2 getGuiPositionNotNull(Category category) {
        return guiPositions.computeIfAbsent(category, category1 -> new Vector2());
    }

    public Vector2 getGuiPosition(Category category) {
        return guiPositions.get(category);
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
        tag.put("guiPositions", NbtUtils.mapToTag(guiPositions));
        tag.put("categoryColors", NbtUtils.mapToTag(categoryColors));

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        version = tag.getString("version");
        prefix = tag.getString("prefix");
        autoCraft.fromTag(tag.getCompound("autoCraft"));
        guiPositions = NbtUtils.mapFromTag(tag.getCompound("guiPositions"), Category::valueOf, tag1 -> new Vector2().fromTag((CompoundTag) tag1));
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
}
