package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AntiAutoAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Forces you to rotate upwards when placing obsidian above you.")
            .defaultValue(true)
            .build()
    );

    public AntiAutoAnvil(){
        super(Category.Combat, "Anti-Auto-Anvil", "Automatically prevents Auto Anvil by placing obsidian above you.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        assert mc.interactionManager != null;
        assert mc.world != null;
        assert mc.player != null;
        for(int i = 2; i <= mc.interactionManager.getReachDistance() + 2; i++){
            if (mc.world.getBlockState(mc.player.getBlockPos().add(0, i, 0)).getBlock() == Blocks.ANVIL
                    && mc.world.getBlockState(mc.player.getBlockPos().add(0, i - 1, 0)).isAir()){
                int slot = InvUtils.findItemWithCount(Items.OBSIDIAN).slot;
                if (rotate.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(mc.player.yaw, -90, mc.player.isOnGround()));
                }
                if (slot != 1 && slot < 9) {
                    PlayerUtils.placeBlock(mc.player.getBlockPos().add(0, i - 2, 0), slot, InvUtils.getHand(Items.OBSIDIAN));
                } else if (mc.player.getOffHandStack().getItem() == Items.OBSIDIAN){
                    PlayerUtils.placeBlock(mc.player.getBlockPos().add(0, i - 2, 0),  InvUtils.getHand(Items.OBSIDIAN));
                }
            }
        }
    });
}
