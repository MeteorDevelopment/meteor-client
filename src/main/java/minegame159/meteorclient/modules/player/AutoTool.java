package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.StartBreakingBlockEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;

public class AutoTool extends ToggleModule {
    public enum Prefer {
        None,
        Fortune,
        SilkTouch
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Prefer> prefer = sgGeneral.add(new EnumSetting.Builder<Prefer>()
            .name("prefer")
            .description("Prefer silk touch, fortune or none.")
            .defaultValue(Prefer.Fortune)
            .build()
    );

    private final Setting<Boolean> preferMending = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Prefers mending.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> enderChestOnlyWithSilkTouch = sgGeneral.add(new BoolSetting.Builder()
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
        Material material = block.getMaterial(null);

        if (item instanceof SwordItem && material == Material.COBWEB) return true;
        if (item instanceof AxeItem && (block == Blocks.SCAFFOLDING || material == Material.WOOD || material == Material.BAMBOO || material == Material.BAMBOO_SAPLING || material == Material.PLANT || material == Material.PUMPKIN))  return true;
        if (item instanceof PickaxeItem && (material == Material.SHULKER_BOX || material == Material.ANVIL || material == Material.CLAY || material == Material.ICE || material == Material.PACKED_ICE || material == Material.METAL || material == Material.PISTON || material == Material.STONE || material == Material.PART)) return true;
        if (item instanceof ShovelItem && (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM || material == Material.EARTH || material == Material.SNOW || material == Material.SNOW_BLOCK)) return true;
        if (item instanceof ShearsItem && (material == Material.WOOL || material == Material.CARPET)) return true;
        if (item instanceof HoeItem && material == Material.ORGANIC) return true;

        return false;
    }
}
