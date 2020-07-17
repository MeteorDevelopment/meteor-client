package minegame159.meteorclient.modules.combat;

//Created by squidoodly 13/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class Auto32K extends ToggleModule {
    public enum Mode{
        Hopper,
        Dispenser
    }

    public Auto32K(){super(Category.Combat, "auto32k", "Does 32k PvP for you.");}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The bypass used.")
            .defaultValue(Mode.Dispenser
            ).build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the shulker is placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> fillHopper = sgGeneral.add(new BoolSetting.Builder()
            .name("fill-hopper")
            .description("Fills all but one slot of the hopper.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Block>> throwawayItems = sgGeneral.add(new BlockListSetting.Builder()
            .name("throwaway-blocks")
            .description("Acceptable blocks to use to fill the hopper")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private int x;
    private int z;

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Hopper) {
            int shulkerSlot = InvUtils.findItemWithCount(Items.SHULKER_BOX).slot;
            int hopperSlot = InvUtils.findItemWithCount(Items.HOPPER).slot;
            if (isValidSlot(shulkerSlot) || isValidSlot(hopperSlot)) return;
            List<BlockPos> sortedBlocks = findValidBlocksHopper();
            sortedBlocks.sort(Comparator.comparingDouble(value -> mc.player.squaredDistanceTo(value.getX(), value.getY(), value.getZ())));
            Iterator<BlockPos> sortedIterator = sortedBlocks.iterator();
            BlockPos bestBlock = sortedIterator.next();

            if (bestBlock != null) {
                mc.player.inventory.selectedSlot = hopperSlot;
                while(!Utils.place(Blocks.HOPPER.getDefaultState(), bestBlock)){
                    bestBlock = sortedIterator.next();
                }
                mc.player.setSneaking(true);
                mc.player.inventory.selectedSlot = shulkerSlot;
                if (!Utils.place(Blocks.SHULKER_BOX.getDefaultState(), bestBlock.up())){ Utils.sendMessage("#redFailed to place."); return;}
                mc.player.setSneaking(false);
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(bestBlock.up()), mc.player.getHorizontalFacing(), bestBlock.up(), false));
            }
        }else if (mode.get() == Mode.Dispenser) {
            int shulkerSlot = InvUtils.findItemWithCount(Items.SHULKER_BOX).slot;
            int hopperSlot = InvUtils.findItemWithCount(Items.HOPPER).slot;
            int dispenserSlot = InvUtils.findItemWithCount(Items.DISPENSER).slot;
            int redstoneSlot = InvUtils.findItemWithCount(Items.REDSTONE_BLOCK).slot;
            if (isValidSlot(shulkerSlot) || isValidSlot(hopperSlot) || isValidSlot(dispenserSlot) || isValidSlot(redstoneSlot))
                return;
            BlockPos bestBlock = findValidBlocksDispenser();
            if (bestBlock != null) {
                mc.player.inventory.selectedSlot = hopperSlot;
                if (!Utils.place(Blocks.HOPPER.getDefaultState(), bestBlock.add(x, 0, z))) {
                    Utils.sendMessage("#redFialed to place");
                    return;
                }
                mc.player.inventory.selectedSlot = dispenserSlot;
                if (x == -1) {
                    //mc.player.yaw = -90f;
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(-90f, mc.player.pitch, mc.player.onGround));
                } else if (x == 1) {
                    //mc.player.yaw = 90f;
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(90f, mc.player.pitch, mc.player.onGround));
                } else if (z == -1) {
                    //mc.player.yaw = 1f;
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(1f, mc.player.pitch, mc.player.onGround));
                } else if (z == 1) {
                    //mc.player.yaw = 179f;
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(179f, mc.player.pitch, mc.player.onGround));
                }
                //mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(bestBlock), Direction.UP, bestBlock, false));
                //mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bestBlock, false)));
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(bestBlock.up()), mc.player.getHorizontalFacing().getOpposite(), bestBlock.up(), false));
            }
        }
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof HopperScreen){
            if (fillHopper.get()) {
                int slot = -1;
                int count = 0;
                Iterator<Block> blocks = throwawayItems.get().iterator();
                for (Item item = blocks.next().asItem(); blocks.hasNext(); item = blocks.next().asItem()){
                    for(int i = 5; i <=  40; i++){
                        ItemStack stack = mc.player.inventory.getInvStack(i);
                        if(stack.getItem() == item && stack.getCount() >= 4){
                            slot = i;
                            count = stack.getCount();
                            break;
                        }
                    }
                    if(count >= 4) break;
                }
                for (int i = 1; i < 5; i++) {
                    if (mc.player.container.getSlot(i).getStack().getItem() instanceof AirBlockItem) {
                        InvUtils.clickSlot(slot + 5, 0, SlotActionType.PICKUP);
                        InvUtils.clickSlot(i, 1, SlotActionType.PICKUP);
                        InvUtils.clickSlot(slot + 5, 0, SlotActionType.PICKUP);
                    }
                }
            }
        }
    });

    private List<BlockPos> findValidBlocksHopper(){
        Iterator<BlockPos> allBlocks = getRange(mc.player.getBlockPos(), placeRange.get()).iterator();
        List<BlockPos> validBlocks = new ArrayList<>();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if(!mc.world.getBlockState(i).getMaterial().isReplaceable()
                    && (mc.world.getBlockState(i.up()).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.up().getX(), i.up().getY(), i.up().getZ(), i.up().getX() + 1.0D, i.up().getY() + 2.0D, i.up().getZ() + 1.0D)).isEmpty())
                    && mc.world.getBlockState(i.up(2)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.up(2).getX(), i.up(2).getY(), i.up(2).getZ(), i.up(2).getX() + 1.0D, i.up(2).getY() + 2.0D, i.up(2).getZ() + 1.0D)).isEmpty()){
                validBlocks.add(i);
            }
        }
        return validBlocks;
    }

    private BlockPos findValidBlocksDispenser(){
        List<BlockPos> allBlocksNotSorted = getRange(mc.player.getBlockPos(), placeRange.get());
        allBlocksNotSorted.sort(Comparator.comparingDouble(value -> mc.player.squaredDistanceTo(value.getX(), value.getY(), value.getZ())));
        Iterator<BlockPos> allBlocks = allBlocksNotSorted.iterator();
        for(BlockPos i = null; allBlocks.hasNext(); i = allBlocks.next()){
            if(i == null) continue;
            if(!mc.world.getBlockState(i).getMaterial().isReplaceable()
                    && (mc.world.getBlockState(i.up()).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.up().getX(), i.up().getY(), i.up().getZ(), i.up().getX() + 1.0D, i.up().getY() + 2.0D, i.up().getZ() + 1.0D)).isEmpty())
                    && (mc.world.getBlockState(i.up(2)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.up(2).getX(), i.up(2).getY(), i.up(2).getZ(), i.up(2).getX() + 1.0D, i.up(2).getY() + 2.0D, i.up(2).getZ() + 1.0D)).isEmpty())
                    && (mc.world.getBlockState(i.up(3)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.up(3).getX(), i.up(3).getY(), i.up(3).getZ(), i.up(2).getX() + 1.0D, i.up(2).getY() + 2.0D, i.up(2).getZ() + 1.0D)).isEmpty())){
                if (mc.world.getBlockState(i.add(-1, 1, 0)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(-1, 1, 0).getX(), i.add(-1, 1, 0).getY(), i.add(-1, 1, 0).getZ(), i.add(-1, 1, 0).getX() + 1.0D, i.add(-1, 1, 0).getY() + 2.0D, i.add(-1, 1, 0).getZ() + 1.0D)).isEmpty()
                        && mc.world.getBlockState(i.add(-1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(-1, 0, 0).getX(), i.add(-1, 0, 0).getY(), i.add(-1, 0, 0).getZ(), i.add(-1, 0, 0).getX() + 1.0D, i.add(-1, 0, 0).getY() + 2.0D, i.add(-1, 0, 0).getZ() + 1.0D)).isEmpty()) {
                    x = -1;
                    z = 0;
                    return i;
                }else if (mc.world.getBlockState(i.add(1, 1, 0)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(1, 1, 0).getX(), i.add(1, 1, 0).getY(), i.add(1, 1, 0).getZ(), i.add(1, 1, 0).getX() + 1.0D, i.add(1, 1, 0).getY() + 2.0D, i.add(1, 1, 0).getZ() + 1.0D)).isEmpty()
                        && mc.world.getBlockState(i.add(1, 0, 0)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(1, 0, 0).getX(), i.add(1, 0, 0).getY(), i.add(1, 0, 0).getZ(), i.add(1, 0, 0).getX() + 1.0D, i.add(1, 0, 0).getY() + 2.0D, i.add(1, 0, 0).getZ() + 1.0D)).isEmpty()) {
                    x = 1;
                    z = 0;
                    return i;
                }else if (mc.world.getBlockState(i.add(0, 1, -1)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(0, 1, -1).getX(), i.add(0, 1, -1).getY(), i.add(0, 1, -1).getZ(), i.add(0, 1, -1).getX() + 1.0D, i.add(0, 1, -1).getY() + 2.0D, i.add(0, 1, -1).getZ() + 1.0D)).isEmpty()
                        && mc.world.getBlockState(i.add(0, 0, -1)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(0, 0, -1).getX(), i.add(0, 0, -1).getY(), i.add(0, 0, -1).getZ(), i.add(0, 0, -1).getX() + 1.0D, i.add(0, 0, -1).getY() + 2.0D, i.add(0, 0, -1).getZ() + 1.0D)).isEmpty()) {
                    x = 0;
                    z = -1;
                    return i;
                }else if (mc.world.getBlockState(i.add(0, 1, 1)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(0, 1, 1).getX(), i.add(0, 1, 1).getY(), i.add(0, 1, 1).getZ(), i.add(0, 1, 1).getX() + 1.0D, i.add(0, 1, 1).getY() + 2.0D, i.add(0, 1, 1).getZ() + 1.0D)).isEmpty()
                        && mc.world.getBlockState(i.add(0, 0, 1)).getBlock() == Blocks.AIR && mc.world.getEntities(null, new Box(i.add(0, 0, 1).getX(), i.add(0, 0, 1).getY(), i.add(0, 0, 1).getZ(), i.add(0, 0, 1).getX() + 1.0D, i.add(0, 0, 1).getY() + 2.0D, i.add(0, 0, 1).getZ() + 1.0D)).isEmpty()) {
                    x = 0;
                    z = 1;
                    return i;
                }
            }
        }
        return null;
    }

    private List<BlockPos> getRange(BlockPos player, double range){
        List<BlockPos> allBlocks = new ArrayList<>();
        for(double i = player.getX() - range; i < player.getX() + range; i++){
            for(double j = player.getZ() - range; j < player.getZ() + range; j++){
                for(int k = player.getY() - 3; k < player.getY() + 3; k++){
                    BlockPos x = new BlockPos(i, k, j);
                    allBlocks.add(x);
                }
            }
        }
        return allBlocks;
    }

    private boolean isValidSlot(int slot){
        return slot == -1 || slot >= 9;
    }
}
