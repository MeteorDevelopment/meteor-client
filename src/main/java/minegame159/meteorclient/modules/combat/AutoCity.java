package minegame159.meteorclient.modules.combat;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.CityUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class AutoCity extends ToggleModule {

    public AutoCity() {
        super(Category.Combat, "auto-city", "Automatically mines a targets closest valid city block.");
    }

    @Override
    public void onActivate() {

        PlayerEntity target = CityUtils.getPlayerTarget();
        BlockPos mineTarget = CityUtils.getTargetBlock();

        if (target == null || mineTarget == null) {
            Chat.error(this, "No target block found… disabling.");
        } else {
            Chat.info(this, "Attempting to city " + target.getGameProfile().getName());

            if (MathHelper.sqrt(mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > mc.interactionManager.getReachDistance()) {
                Chat.error(this, "Target block out of reach… disabling.");
                toggle();
                return;
            }

            int pickSlot = -1;
            for (int i = 0; i < 9; i++) {
                Item item = mc.player.inventory.getStack(i).getItem();

                if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
                    pickSlot = i;
                    break;
                }
            }
            if (pickSlot == -1) {
                Chat.error(this, "No pick found… disabling.");
                toggle();
                return;
            }

            mc.player.inventory.selectedSlot = pickSlot;

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mineTarget, Direction.UP));
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mineTarget, Direction.UP));

        }
        toggle();
    }
}
