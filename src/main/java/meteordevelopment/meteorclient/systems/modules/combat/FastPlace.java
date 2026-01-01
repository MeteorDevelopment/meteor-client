package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class FastPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("放置冷却（tick）")
        .defaultValue(0)
        .min(0)
        .sliderMax(4)
        .build()
    );

    public FastPlace() {
        super(Categories.Player, "fast-place", "快速放置方块，排除水和岩浆");
    }

    public boolean shouldFastPlace(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        Block block = bi.getBlock();
        // 排除水和岩浆方块
        return block != Blocks.WATER && block != Blocks.LAVA;
    }

    public int getCooldown() {
        return cooldown.get();
    }
}
