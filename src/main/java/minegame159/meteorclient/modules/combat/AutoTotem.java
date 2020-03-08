package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoTotem extends Module {
    private int totemCount;
    private String totemCountString = "0";

    public AutoTotem() {
        super(Category.Combat, "auto-totem", "Automatically equips totems.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof ContainerScreen<?>) return;

        boolean foundTotem = false;
        int preTotemCount = totemCount;
        totemCount = 0;

        for (int i = 0; i < 4 * 9; i++) {
            ItemStack itemStack = mc.player.inventory.getInvStack(i);
            if (itemStack.getItem() != Items.TOTEM_OF_UNDYING) continue;
            totemCount += itemStack.getCount();

            if (!foundTotem && mc.player.getOffHandStack().isEmpty()) {
                mc.interactionManager.method_2906(0, Utils.invIndexToSlotId(i), 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.method_2906(0, Utils.offhandSlotId, 0, SlotActionType.PICKUP, mc.player);
                foundTotem = true;
            }
        }

        if (totemCount != preTotemCount) totemCountString = Integer.toString(totemCount);
    });

    @Override
    public String getInfoString() {
        return totemCountString;
    }
}
