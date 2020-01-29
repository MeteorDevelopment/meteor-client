package minegame159.meteorclient.modules.combat;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
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

    @SubscribeEvent
    private void onTick(TickEvent e) {
        if (mc.currentScreen instanceof ContainerScreen<?>) return;

        boolean foundTotem = false;
        int preTotemCount = totemCount;
        totemCount = 0;

        for (int i = 0; i < 4 * 9; i++) {
            ItemStack itemStack = mc.player.inventory.getInvStack(i);
            if (itemStack.getItem() != Items.TOTEM_OF_UNDYING) continue;
            totemCount += itemStack.getCount();

            if (!foundTotem && mc.player.getOffHandStack().isEmpty()) {
                mc.interactionManager.clickSlot(0, i, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                foundTotem = true;
            }
        }

        if (totemCount != preTotemCount) totemCountString = Integer.toString(totemCount);
    }

    @Override
    public String getInfoString() {
        return totemCountString;
    }
}
