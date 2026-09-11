package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.HashSet;
import java.util.Set;

public class AdvancedStashFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minStorage = sgGeneral.add(new IntSetting.Builder()
        .name("min-storage")
        .description("Minimum storage blocks to count as stash.")
        .defaultValue(8)
        .min(1)
        .build()
    );

    private final Setting<Boolean> checkShulkers = sgGeneral.add(new BoolSetting.Builder()
        .name("check-shulkers")
        .description("Include shulker boxes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkBarrels = sgGeneral.add(new BoolSetting.Builder()
        .name("check-barrels")
        .description("Include barrels.")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> foundStashes = new HashSet<>();
    private int timer;

    public AdvancedStashFinder() {
        super(Categories.Donut, "advanced-stash-finder", "Advanced stash detection with multiple storage types.");
    }

    @Override
    public void onActivate() {
        foundStashes.clear();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        // A 33-block cube is ~36k lookups; once a second is plenty for finding stashes.
        if (++timer < 20) return;
        timer = 0;

        BlockPos playerPos = mc.player.blockPosition();
        int storageCount = 0;
        BlockPos center = null;

        for (int x = -16; x <= 16; x++) {
            for (int y = -16; y <= 16; y++) {
                for (int z = -16; z <= 16; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (isStorage(mc.level.getBlockState(pos).getBlock())) {
                        storageCount++;
                        if (center == null) center = pos;
                    }
                }
            }
        }

        if (storageCount >= minStorage.get() && center != null && foundStashes.add(center)) {
            info("Stash found! %d storage blocks at %d, %d, %d",
                storageCount, center.getX(), center.getY(), center.getZ());
        }
    }

    private boolean isStorage(Block block) {
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) return true;
        if (checkBarrels.get() && block == Blocks.BARREL) return true;
        return checkShulkers.get() && block instanceof ShulkerBoxBlock;
    }
}
