package minegame159.meteorclient.modules.player;

//Created by squidoodly 8/05/2020
//Updated by squidoodly 15/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoReplenish extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("The amount this actives at")
            .defaultValue(32)
            .min(1)
            .sliderMax(63)
            .build()
    );

    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder()
            .name("offhand")
            .description("Whether to re-fill your offhand")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder()
            .name("alert")
            .description("Send messages in chat when you run out of items")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> unstackable = sgGeneral.add(new BoolSetting.Builder()
            .name("unstackable")
            .description("Replenishes unstackable items (only works for main hand and offhand)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> searchHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("search-hotbar")
            .description("Refills items if they are in your hotbar.")
            .defaultValue(true)
            .build()
    );

    private final List<Item> items = new ArrayList<>();

    private Item lastMainHand, lastOffHand;
    private int lastSlot;

    /*private ItemStack item;
    private ItemStack offHandItem;
    private int lastSlot;*/

    public AutoReplenish(){
        super(Category.Player, "auto-replenish", "Automatically fills your hotbar and offhand items");
    }

    @Override
    public void onActivate() {
        lastSlot = mc.player.inventory.selectedSlot;
    }

    @Override
    public void onDeactivate() {
        lastMainHand = lastOffHand = null;
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        if(mc.currentScreen instanceof ContainerScreen) return;

        // Hotbar, stackable items
        for (int i = 0; i < 9; i++) {
            replenishStackableItems(mc.player.inventory.getInvStack(i), i);
        }

        // OffHand, stackable items
        if (offhand.get()) {
            replenishStackableItems(mc.player.getOffHandStack(), InvUtils.OFFHAND_SLOT);
        }

        // MainHand, unstackable items
        if (unstackable.get()) {
            ItemStack mainHandItemStack = mc.player.getMainHandStack();
            if (mainHandItemStack.getItem() != lastMainHand && mainHandItemStack.isEmpty() && (lastMainHand != null && lastMainHand != Items.AIR) && isUnstackable(lastMainHand) && mc.player.inventory.selectedSlot == lastSlot) {
                int slot = findSlot(lastMainHand);
                if (slot != -1) moveItems(slot, lastSlot, false);
            }
            lastMainHand = mc.player.getMainHandStack().getItem();
            lastSlot = mc.player.inventory.selectedSlot;

            if (offhand.get()) {
                // OffHand, unstackable items
                ItemStack offHandItemStack = mc.player.getOffHandStack();
                if (offHandItemStack.getItem() != lastOffHand && offHandItemStack.isEmpty() && (lastOffHand != null && lastOffHand != Items.AIR) && isUnstackable(lastOffHand)) {
                    int slot = findSlot(lastOffHand);
                    if (slot != -1) moveItems(slot, InvUtils.OFFHAND_SLOT, false);
                }
                lastOffHand = mc.player.getOffHandStack().getItem();
            }
        }
    });

    @EventHandler
    private final Listener<OpenScreenEvent> onScreen = new Listener<>(event -> {
        if(mc.currentScreen instanceof ContainerScreen && !(mc.currentScreen instanceof AbstractInventoryScreen)){
            items.clear();
        }
    });

    private void replenishStackableItems(ItemStack itemStack, int i) {
        if(itemStack.isEmpty() || !itemStack.isStackable()) return;

        if(itemStack.getCount() < amount.get()) {
            int slot = findSlot(itemStack.getItem());
            if(slot == -1) return;

            moveItems(slot, i, true);
        }
    }

    private void moveItems(int from, int to, boolean stackable) {
        InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(InvUtils.invIndexToSlotId(to), 0, SlotActionType.PICKUP);
        if (stackable) InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
    }

    private int findSlot(Item item) {
        int slot = findItems(item);

        if(slot == -1 && !items.contains(item)){
            if(alert.get()) {
                Utils.sendMessage("#redYou are out of #blue" + item.toString() + "#red. Cannot refill.");
            }

            items.add(item);
        }

        return slot;
    }

    private int findItems(Item item) {
        int slot = -1;

        for (int i = searchHotbar.get() ? 0 : 9; i < mc.player.inventory.main.size(); i++) {
            if (mc.player.inventory.main.get(i).getItem() == item && (!searchHotbar.get() || i != mc.player.inventory.selectedSlot)) {
                slot = i;
                return slot;
            }
        }

        return slot;
    }

    private boolean isUnstackable(Item item) {
        return item.getMaxCount() <= 1;
    }
}
