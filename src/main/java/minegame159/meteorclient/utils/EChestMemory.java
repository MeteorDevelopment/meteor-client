package minegame159.meteorclient.utils;

import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.BlockActivateEvent;
import minegame159.meteorclient.events.OpenScreenEvent;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.container.GenericContainer;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;

public class EChestMemory {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static int echestOpenedState;
    public static final DefaultedList<ItemStack> ITEMS = DefaultedList.ofSize(27, ItemStack.EMPTY);

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(onBlockActivate);
        MeteorClient.EVENT_BUS.subscribe(onOpenScreenEvent);
    }

    private static final Listener<BlockActivateEvent> onBlockActivate = new Listener<>(event -> {
        if (event.blockState.getBlock() instanceof EnderChestBlock && echestOpenedState == 0) echestOpenedState = 1;
    });

    private static final Listener<OpenScreenEvent> onOpenScreenEvent = new Listener<>(event -> {
        if (echestOpenedState == 1 && event.screen instanceof GenericContainerScreen) {
            echestOpenedState = 2;
            return;
        }
        if (echestOpenedState == 0) return;

        if (!(MC.currentScreen instanceof GenericContainerScreen)) return;
        GenericContainer container = ((GenericContainerScreen) MC.currentScreen).getContainer();
        if (container == null) return;
        Inventory inv = container.getInventory();

        for (int i = 0; i < 27; i++) {
            ITEMS.set(i, inv.getInvStack(i));
        }

        echestOpenedState = 0;
    });
}
