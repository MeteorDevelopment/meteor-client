package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.ItemListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class AutoDrop extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to drop.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> excludeHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("exclude-hotbar")
            .description("Doesn't drop items from hotbar.")
            .defaultValue(false)
            .build()
    );

    public AutoDrop() {
        super(Category.Player, "auto-drop", "Automatically drops selected items.");
    }

    @EventHandler
    private Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (mc.currentScreen instanceof HandledScreen<?>) return;

        for (int i = excludeHotbar.get() ? 9 : 0; i < mc.player.inventory.size(); i++) {
            if (items.get().contains(mc.player.inventory.getStack(i).getItem())) {
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(i), 1, SlotActionType.THROW);
            }
        }
    });
}
