package minegame159.meteorclient.modules.misc;

//Created by squidoodly 20/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.player.AutoTool;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

public class EChestFarmer extends ToggleModule {
    public EChestFarmer(){
        super(Category.Misc, "EChestFarmer", "Farms EChests for obby.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        InvUtils.FindItemResult itemResult = InvUtils.findItemWithCount(Items.ENDER_CHEST);
        int slot = -1;
        if(itemResult.count != 0 && itemResult.slot < 9) {
            for (int i = 0; i < 9; i++) {
                if (ModuleManager.INSTANCE.get(AutoTool.class).isEffectiveOn(mc.player.inventory.getInvStack(i).getItem(), Blocks.ENDER_CHEST)
                        && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, mc.player.inventory.getInvStack(i)) == 0) {
                    slot = i;
                }
            }
            if (slot != -1) {
                int x = 0;
                int z = 0;
                if(mc.world.getBlockState(mc.player.getBlockPos().add(1, 0, 0)).getBlock().equals(Blocks.AIR)){
                    x =1;
                }else if(mc.world.getBlockState(mc.player.getBlockPos().add(-1, 0, 0)).getBlock().equals(Blocks.AIR)){
                    x = -1;
                }else if(mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, 1)).getBlock().equals(Blocks.AIR)){
                    z = 1;
                }else if(mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, -1)).getBlock().equals(Blocks.AIR)){
                    z = -1;
                }
                if(x != 0 || z != 0) {
                    mc.player.inventory.selectedSlot = itemResult.slot;
                    PlayerInteractBlockC2SPacket placePacket = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, mc.player.getBlockPos().add(x, 0, z), false));
                    mc.player.networkHandler.sendPacket(placePacket);
                }
            }
        }
        int x = 0;
        int z = 0;
        if(mc.world.getBlockState(mc.player.getBlockPos().add(1, 0, 0)).getBlock().equals(Blocks.ENDER_CHEST)){
            x =1;
        }else if(mc.world.getBlockState(mc.player.getBlockPos().add(-1, 0, 0)).getBlock().equals(Blocks.ENDER_CHEST)){
            x = -1;
        }else if(mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, 1)).getBlock().equals(Blocks.ENDER_CHEST)){
            z = 1;
        }else if(mc.world.getBlockState(mc.player.getBlockPos().add(0, 0, -1)).getBlock().equals(Blocks.ENDER_CHEST)){
            z = -1;
        }
        if(x != 0 || z != 0) {
            if(slot >= 0) {
                mc.player.inventory.selectedSlot = slot;
            }
            mc.interactionManager.updateBlockBreakingProgress(mc.player.getBlockPos().add(x, 0, z), Direction.UP);
        }
    });
}
