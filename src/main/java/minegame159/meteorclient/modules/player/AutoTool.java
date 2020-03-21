package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.StartBreakingBlockEvent;
import minegame159.meteorclient.mixininterface.IMiningToolItem;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;

public class AutoTool extends Module {
    public enum Prefer {
        None,
        Fortune,
        SilkTouch
    }

    private Setting<Prefer> prefer = addSetting(new EnumSetting.Builder<Prefer>()
            .name("prefer")
            .description("Prefer silk touch, fortune or none.")
            .defaultValue(Prefer.Fortune)
            .build()
    );

    private Setting<Boolean> preferMending = addSetting(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Prefers mending.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> enderChestOnlyWithSilkTouch = addSetting(new BoolSetting.Builder()
            .name("ender-chest-only-with-silk-touch")
            .description("Mine ender chest only wiht silk touch.")
            .defaultValue(true)
            .build()
    );

    public AutoTool() {
        super(Category.Player, "auto-tool", "Automatically switches to the most effective tool when breaking blocks.");
    }

    @EventHandler
    private Listener<StartBreakingBlockEvent> onStartBreakingBlock = new Listener<>(event -> {
        BlockState blockState = mc.world.getBlockState(event.blockPos);

        int bestScore = -1;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getInvStack(i);
            if (!isEffectiveOn(itemStack.getItem(), blockState.getBlock())) continue;
            int score = 0;

            if (enderChestOnlyWithSilkTouch.get() && blockState.getBlock() == Blocks.ENDER_CHEST && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) continue;

            score += Math.round(itemStack.getMiningSpeed(blockState));
            score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
            score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
            if (preferMending.get()) score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
            if (prefer.get() == Prefer.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
            if (prefer.get() == Prefer.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            mc.player.inventory.selectedSlot = bestSlot;
        }
    });

    private boolean isEffectiveOn(Item item, Block block) {
        if (item instanceof SwordItem && block == Blocks.COBWEB) return true;
        if (item instanceof AxeItem && (block instanceof ChestBlock || block == Blocks.BARREL)) return true;
        if (item instanceof PickaxeItem && block == Blocks.ENDER_CHEST) return true;
        if (item instanceof MiningToolItem) return ((IMiningToolItem) item).isEffectiveOn(block);
        return false;
    }
}
