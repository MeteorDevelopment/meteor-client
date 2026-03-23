package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.GenericSetting;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.settings.IGeneric;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;

public class ESPBlockData implements IGeneric<ESPBlockData>, IChangeable, IBlockData<ESPBlockData> {
    public ShapeMode shapeMode;
    public boolean tracer;
    public boolean changed;

    public ESPBlockData(ShapeMode shapeMode, boolean tracer) {
        this.shapeMode = shapeMode;
        this.tracer = tracer;
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(theme, this, block, setting);
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, GenericSetting<ESPBlockData> setting) {
        return new ESPBlockDataScreen(theme, this, setting);
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    public void changed() {
        changed = true;
    }

    @Override
    public ESPBlockData set(ESPBlockData value) {
        shapeMode = value.shapeMode;
        tracer = value.tracer;
        changed = value.changed;
        return this;
    }

    @Override
    public ESPBlockData copy() {
        return new ESPBlockData(shapeMode, tracer);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("shapeMode", shapeMode.name());
        tag.putBoolean("tracer", tracer);
        tag.putBoolean("changed", changed);

        return tag;
    }

    @Override
    public ESPBlockData fromTag(NbtCompound tag) {
        shapeMode = ShapeMode.valueOf(tag.getString("shapeMode", ""));
        tracer = tag.getBoolean("tracer", false);
        changed = tag.getBoolean("changed", false);

        return this;
    }
}
