package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WItemWithLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class BlockListSettingScreen extends LeftRightListSettingScreen<Block> {
    public BlockListSettingScreen(Setting<List<Block>> setting) {
        super("Select blocks", setting, Registry.BLOCK);
    }

    @Override
    protected boolean includeValue(Block value) {
        return value != Blocks.AIR;
    }

    @Override
    protected WWidget getValueWidget(Block value) {
        return new WItemWithLabel(value.asItem().getDefaultStack());
    }

    @Override
    protected String getValueName(Block value) {
        return value.getName().getString();
    }
}
