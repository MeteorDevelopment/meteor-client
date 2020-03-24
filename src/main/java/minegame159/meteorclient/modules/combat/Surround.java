package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class Surround extends Module {
    public static Surround INSTANCE;

    private Setting<Boolean> onlyOnGround = addSetting(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Works only when you standing on ground.")
            .defaultValue(true)
            .build()
    );

    public Surround() {
        super(Category.Combat, "surround", "Surrounds you with obsidian (or other blocks) to take less damage.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (onlyOnGround.get() && !mc.player.onGround) return;

        int slot = -1;

        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getInvStack(i).getItem();

            if (!(item instanceof BlockItem)) continue;

            if (item == Items.OBSIDIAN) {
                slot = i;
                break;
            }

            if (((BlockItem) item).getBlock().getDefaultState().isSimpleFullBlock(mc.world, null)) slot = i;
        }

        if (slot != -1) {
            int preSelectedSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            BlockState blockState = ((BlockItem) mc.player.inventory.getMainHandStack().getItem()).getBlock().getDefaultState();

            Utils.place(blockState, mc.player.getBlockPos().add(1, 0, 0));
            Utils.place(blockState, mc.player.getBlockPos().add(-1, 0, 0));
            Utils.place(blockState, mc.player.getBlockPos().add(0, 0, 1));
            Utils.place(blockState, mc.player.getBlockPos().add(0, 0, -1));

            mc.player.inventory.selectedSlot = preSelectedSlot;
        }
    });
}
