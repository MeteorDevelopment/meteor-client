package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SelfWeb extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("double")
            .description("Places in your upper hitbox as well as your lower.")
            .defaultValue(false)
            .build()
    );

    public SelfWeb() {
        super(Category.Combat, "self-web", "Automatically places webs at your feet.");
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.COBWEB) {
                webSlot = i;
                break;
            }
        }
        if (webSlot == -1) return;

        int prevSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = webSlot;
        BlockPos playerPos = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(playerPos).isAir()) {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, mc.player.getBlockPos().down(), true));
        }
        if (doubles.get() && mc.world.getBlockState(playerPos.add(0, 1, 0)).isAir()) {
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, mc.player.getBlockPos().add(0, 1, 0), true));
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.inventory.selectedSlot = prevSlot;
    });
}
