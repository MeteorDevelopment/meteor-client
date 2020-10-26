package minegame159.meteorclient;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.NbtUtils;
import minegame159.meteorclient.utils.Savable;
import net.minecraft.nbt.CompoundTag;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Config extends Savable<Config> {
    public static Config INSTANCE;

    public final Version version = new Version("0.3.6");
    private String prefix = ".";
    public GuiConfig guiConfig = new GuiConfig();

    public boolean chatCommandsInfo = true;

    private Map<Category, Color> categoryColors = new HashMap<>();

    public Config() {
        super(new File(MeteorClient.FOLDER, "config.nbt"));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public String getPrefix() {
        return prefix;
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

        tag.putString("version", version.getOriginalString());
        tag.putString("prefix", prefix);
        tag.put("categoryColors", NbtUtils.mapToTag(categoryColors));
        tag.put("guiConfig", guiConfig.toTag());
        tag.putBoolean("chatCommandsInfo", chatCommandsInfo);

        return tag;
    }

    @Override
    public Config fromTag(CompoundTag tag) {
        prefix = tag.getString("prefix");
        categoryColors = NbtUtils.mapFromTag(tag.getCompound("categoryColors"), Category::valueOf, tag1 -> new Color().fromTag((CompoundTag) tag1));
        guiConfig.fromTag(tag.getCompound("guiConfig"));
        chatCommandsInfo = !tag.contains("chatCommandsInfo") || tag.getBoolean("chatCommandsInfo");

        // In 0.2.9 the default font was changed, detect when people load up 0.2.9 for the first time
        Version lastVer = new Version(tag.getString("version"));
        Version v029 = new Version("0.2.9");

        if (lastVer.isLowerThan(v029) && version.isAtLeast(v029)) {
            MeteorClient.INSTANCE.resetFont();
        }

        return this;
    }
}
