package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoTrap extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum topMode {
        Full,
        Top,
        None
    }

    public enum bottomMode {
        Single,
        Platform,
        None
    }

    private final Setting<topMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<topMode>()
            .name("top-mode")
            .description("Which blocks to place on the top half of the target.")
            .defaultValue(topMode.Full)
            .build()
    );

    private final Setting<bottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<bottomMode>()
            .name("bottom-mode")
            .description("Which blocks to place on the bottom half of the target.")
            .defaultValue(bottomMode.Single)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("turn-off")
            .description("Turns off when placed.")
            .defaultValue(false)
            .build()
    );

    public AutoTrap(){
        super(Category.Combat, "auto-trap", "Traps people in an obsidian cage.");
    }

    private PlayerEntity target = null;
    private BlockPos targetPosUp;
    private BlockPos targetPos;
    private int obsidianSlot;
    private int prevSlot;
    private int blocksPlaced;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        obsidianSlot = -1;
        target = null;
        for(int i = 0; i < 9; i++){
            if (mc.player.inventory.getStack(i).getItem() == Blocks.OBSIDIAN.asItem()){
                obsidianSlot = i;
                break;
            }
        }
        if (obsidianSlot == -1) return;
        for(PlayerEntity player : mc.world.getPlayers()){
            if (player == mc.player || !FriendManager.INSTANCE.attack(player)) continue;
            if (target == null){
                target = player;
            }else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)){
                target = player;
            }
        }
        if (target == null) return;
        if (mc.player.distanceTo(target) < 4){
            prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = obsidianSlot;
            targetPosUp = target.getBlockPos().up();
            targetPos = target.getBlockPos();

            //PLACEMENT
            switch(topPlacement.get()) {
                case Full:
                    blocksPlaced = 0;
                    if(mc.world.getBlockState(targetPosUp.add(0, 1, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos(), Direction.UP, targetPosUp.add(0, 1, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPosUp.add(1, 0, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos(), Direction.UP, targetPosUp.add(1, 0, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPosUp.add(-1, 0, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos(), Direction.UP, targetPosUp.add(-1, 0, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPosUp.add(0, 0, 1)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos(), Direction.UP, targetPosUp.add(0, 0, 1), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPosUp.add(0, 0, -1)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos(), Direction.UP, targetPosUp.add(0, 0, -1), false));
                        blocksPlaced++;
                    }
                    if (blocksPlaced >= 1) mc.player.swingHand(Hand.MAIN_HAND);
                    blocksPlaced = 0;
                    break;
                case Top:
                    if(mc.world.getBlockState(targetPosUp.add(0, 1, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(0, 1, 0), Direction.UP, targetPosUp.add(0, 1, 0), false));
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                    break;
                case None:
            }

            switch(bottomPlacement.get()) {
                case Platform:
                    blocksPlaced = 0;
                    if(mc.world.getBlockState(targetPos.add(0, -1, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(0, -1, 0), Direction.DOWN, targetPos.add(0, -1, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPos.add(1, -1, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(1, -1, 0), Direction.DOWN, targetPos.add(1, -1, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPos.add(-1, -1, 0)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(-1, -1, 0), Direction.DOWN, targetPos.add(-1, -1, 0), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPos.add(0, -1, 1)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(0, -1, 1), Direction.DOWN, targetPos.add(0, -1, 1), false));
                        blocksPlaced++;
                    }
                    if(mc.world.getBlockState(targetPos.add(0, -1, -1)).getMaterial().isReplaceable()){
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add(0, -1, -1), Direction.DOWN, targetPos.add(0, -1, -1), false));
                        blocksPlaced++;
                    }
                    if (blocksPlaced >= 1) mc.player.swingHand(Hand.MAIN_HAND);
                    blocksPlaced = 0;
                    break;
                case Single:
                    if (mc.world.getBlockState(targetPos.add(0, -1, 0)).isAir()) {
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(target.getPos().add( 0, -1, 0), Direction.DOWN, targetPos.add(0, -1, 0), true));
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                    break;
                case None:
            }
            if (turnOff.get()) toggle();
            mc.player.inventory.selectedSlot = prevSlot;
        }
    });
}