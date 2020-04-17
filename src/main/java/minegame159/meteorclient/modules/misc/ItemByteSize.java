package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;

public class ItemByteSize extends ToggleModule {
    public enum Mode {
        Standard, True
    }

    private Setting<Mode> mode;
    private Setting<Boolean> useKbIfBigEnough = addSetting(new BoolSetting.Builder()
            .name("use-kb-if-big-enough")
            .description("Uses kilobytes instead of bytes if the item is larget than 1 kb.")
            .defaultValue(true)
            .onChanged(aBoolean -> mode.setVisible(aBoolean))
            .build()
    );

    public ItemByteSize() {
        super(Category.Misc, "item-byte-size", "Displays item's size in bytes in tooltip.");

        mode = addSetting(new EnumSetting.Builder<Mode>()
                .name("mode")
                .description("Standard 1 kb = 1000 b, True 1 kb = 1024 b.")
                .defaultValue(Mode.True)
                .build()
        );
    }

    private int getKbSize() {
        return mode.get() == Mode.True ? 1024 : 1000;
    }

    public String bytesToString(int count) {
        if (useKbIfBigEnough.get() && count >= getKbSize()) return String.format("%.2f kb", count / (float) getKbSize());
        return String.format("%d bytes", count);
    }
}
