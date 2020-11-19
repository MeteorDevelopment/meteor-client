package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
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

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away from you it will place.")
            .defaultValue(4)
            .min(1)
            .max(10)
            .build()
    );

    public LiquidFiller(){
        super(Category.Misc, "Liquid-Filler", "Places blocks inside of liquid source blocks within range of you.");
    }

    private int slot;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        BlockPos player = mc.player.getBlockPos();
        int rangeInt = range.get().intValue();
        for (int y = -Math.min(rangeInt, player.getY()); y < Math.min(rangeInt, 255 - player.getY()); ++y) {
            for (int x = -rangeInt; x < rangeInt; ++x) {
                for (int z = -rangeInt; z < rangeInt; ++z) {
                    BlockPos pos = player.add(x, y, z);
                    Block liquid = mc.world.getBlockState(pos).getBlock();
                    if (mc.world.getBlockState(pos).getFluidState().getLevel() == 8 && this.mc.world.getBlockState(pos).getFluidState().isStill()) {
                        slot = getSlot();
                        if (slot == -1) return;
                        int prevSlot = mc.player.inventory.selectedSlot;
                        mc.player.inventory.selectedSlot = slot;

                        switch (placeInLiquids.get()) {
                            case Lava:
                                if (liquid == Blocks.LAVA) {
                                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                                break;
                            case Water:
                                if (liquid == Blocks.WATER) {
                                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                                break;
                            case Both:
                                if (mc.world.getBlockState(pos).getMaterial().isLiquid()) {
                                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, pos, true));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                }
                                break;
                        }
                        mc.player.inventory.selectedSlot = prevSlot;
                    }
                }
            }
            break;
        }
    });


    private int getSlot() {
        slot = -1;
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