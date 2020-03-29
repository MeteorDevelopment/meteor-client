package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class XpBottleThrower extends ToggleModule {
    public XpBottleThrower() {
        super(Category.Player, "xp-bottle-thrower", "Automatically throws xp bottles in your hotbar.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getInvStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            int preSelectedSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = slot;
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            mc.player.inventory.selectedSlot = preSelectedSlot;
        }
    });
}
