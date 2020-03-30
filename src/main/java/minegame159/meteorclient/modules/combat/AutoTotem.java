package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Items;

public class AutoTotem extends ToggleModule {
    private int totemCount;
    private String totemCountString = "0";

    public AutoTotem() {
        super(Category.Combat, "auto-totem", "Automatically equips totems.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof ContainerScreen<?>) return;

        int preTotemCount = totemCount;
        InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);

        if (result.found() && mc.player.getOffHandStack().isEmpty()) {
            InvUtils.clickSlot(InvUtils.invIndexToSlotId(result.slot), 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(InvUtils.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
        }

        if (result.count != preTotemCount) totemCountString = Integer.toString(result.count);
    });

    @Override
    public String getInfoString() {
        return totemCountString;
    }
}
