package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.ItemListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class AutoDrop extends ToggleModule {
    private Setting<List<Item>> items = addSetting(new ItemListSetting.Builder()
            .name("items")
            .description("Items to drop.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    public AutoDrop() {
        super(Category.Player, "auto-drop", "Automatically drops selected items.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen != null) return;

        for (int i = 0; i < mc.player.inventory.getInvSize(); i++) {
            if (items.get().contains(mc.player.inventory.getInvStack(i).getItem())) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
            }
        }
    });
}
