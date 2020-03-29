package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.BlockListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlockListSetting extends Setting<List<Block>> {
    public BlockListSetting(String name, String description, String group, List<Block> defaultValue, Consumer<List<Block>> onChanged, Consumer<Setting<List<Block>>> onModuleActivated) {
        super(name, description, group, defaultValue, onChanged, onModuleActivated);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new BlockListSettingScreen(this));
    }

    @Override
    protected List<Block> parseImpl(String str) {
        String[] values = str.split(",");
        List<Block> blocks = new ArrayList<>(1);

        for (String value : values) {
            String val = value.trim();
            Identifier id;
            if (val.contains(":")) id = new Identifier(val);
            else id = new Identifier("minecraft", val);
            blocks.add(Registry.BLOCK.get(id));
        }

        return blocks;
    }

    @Override
    protected void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<Block> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "#blueblock id #gray(dirt, minecraft:stone, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (Block block : get()) {
            valueTag.add(new StringTag(Registry.BLOCK.getId(block).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<Block> fromTag(CompoundTag tag) {
        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            get().add(Registry.BLOCK.get(new Identifier(tagI.asString())));
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private String group;
        private List<Block> defaultValue;
        private Consumer<List<Block>> onChanged;
        private Consumer<Setting<List<Block>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder defaultValue(List<Block> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<Block>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<Block>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public BlockListSetting build() {
            return new BlockListSetting(name, description, group, defaultValue, onChanged, onModuleActivated);
        }
    }
}
