package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class StorageStealer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between taking each stack.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> autoClose = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-close")
        .description("Close the container once it is empty or your inventory is full.")
        .defaultValue(true)
        .build()
    );

    private int timer;

    public StorageStealer() {
        super(Categories.Donut, "storage-stealer", "Takes every stack out of any container you open.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (timer > 0) {
            timer--;
            return;
        }

        if (mc.player == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?>)) return;

        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == mc.player.inventoryMenu) return;

        int containerSlots = menu.slots.size() - 36;
        for (int i = 0; i < containerSlots; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) continue;

            // Shift-clicking with a full inventory moves nothing; stop instead of looping forever.
            if (!InvUtils.findEmpty().found()) break;

            InvUtils.shiftClick().slotId(i);
            timer = delay.get();
            return;
        }

        if (autoClose.get()) mc.player.closeContainer();
    }
}
