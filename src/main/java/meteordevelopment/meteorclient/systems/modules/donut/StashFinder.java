/*
 * Ember Client - Donut SMP Module
 */

package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

public class StashFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minChests = sgGeneral.add(new IntSetting.Builder()
        .name("min-chests")
        .description("Minimum chests to count as a stash.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Range to check for chests.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Send chat notification when stash found.")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> foundStashes = new HashSet<>();

    public StashFinder() {
        super(Categories.Donut, "stash-finder", "Finds potential stashes on Donut SMP.");
    }

    @Override
    public void onActivate() {
        foundStashes.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        BlockPos playerPos = mc.player.blockPosition();
        int chestCount = 0;
        BlockPos centerPos = null;
        int r = range.get();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (mc.level.getBlockState(pos).getBlock() == Blocks.CHEST) {
                        chestCount++;
                        if (centerPos == null) centerPos = pos;
                    }
                }
            }
        }

        if (chestCount >= minChests.get() && centerPos != null && !foundStashes.contains(centerPos)) {
            foundStashes.add(centerPos);
            if (notify.get()) {
                info("Potential stash found! %d chests at %d, %d, %d",
                    chestCount, centerPos.getX(), centerPos.getY(), centerPos.getZ());
            }
        }
    }
}
