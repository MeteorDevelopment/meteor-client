package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.StorageBlockListSetting;
import net.minecraft.block.entity.BlockEntityType;

import java.util.List;

public class StorageBlockListSettingScreen extends WindowScreen {
    public StorageBlockListSettingScreen(Setting<List<BlockEntityType<?>>> setting) {
        super("Select storage blocks", true);

        for (int i = 0; i < StorageBlockListSetting.STORAGE_BLOCKS.length; i++) {
            BlockEntityType<?> type = StorageBlockListSetting.STORAGE_BLOCKS[i];
            String name = StorageBlockListSetting.STORAGE_BLOCK_NAMES[i];

            add(new WLabel(name));
            add(new WCheckbox(setting.get().contains(type))).fillX().right().getWidget().action = checkbox -> {
                if (checkbox.checked && !setting.get().contains(type)) {
                    setting.get().add(type);
                    setting.changed();
                } else if (!checkbox.checked && setting.get().remove(type)) {
                    setting.changed();
                }
            };

            row();
        }
    }
}
