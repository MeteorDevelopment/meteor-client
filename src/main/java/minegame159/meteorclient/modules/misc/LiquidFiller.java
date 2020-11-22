package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.BlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class LiquidFiller extends ToggleModule {
    public enum PlaceIn {
        Lava,
        Water,
        Both
    }

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
            .name("block-whitelist")
            .description("Select which blocks it will use to place.")
            .defaultValue(new ArrayList<>())
            .build()
    );

    private final Setting<PlaceIn> placeInLiquids = sgGeneral.add(new EnumSetting.Builder<PlaceIn>()
            .name("place-in")
            .description("Which liquids to place in.")
            .defaultValue(PlaceIn.Lava)
            .build()
    );

    public LiquidFiller(){
        super(Category.Misc, "Liquid-Filler", "Places blocks inside of liquid source blocks within range of you.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
        Block liquid = blockState.getBlock();

        if (blockState.getFluidState().getLevel() == 8 && blockState.getFluidState().isStill()) {
            int slot = getSlot();
            if (slot == -1) return;

            int prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;

            switch (placeInLiquids.get()) {
                case Lava:
                    if (liquid == Blocks.LAVA) {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, blockPos, false));
                    }
                    break;
                case Water:
                    if (liquid == Blocks.WATER) {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, blockPos, false));
                    }
                    break;
                case Both:
                    if (blockState.getMaterial().isLiquid()) {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, blockPos, false));
                    }
                    break;
            }

            mc.player.inventory.selectedSlot = prevSlot;
            BlockIterator.disableCurrent();
        }
    }));

    private int getSlot() {
        int slot = -1;

        for (int i = 0; i < 9; i++){
            ItemStack block = mc.player.inventory.getStack(i);
            if ((block.getItem() instanceof BlockItem) && whitelist.get().contains(Block.getBlockFromItem(block.getItem()))) {
                slot = i;
                break;
            }
        }

        return slot;
    }
}