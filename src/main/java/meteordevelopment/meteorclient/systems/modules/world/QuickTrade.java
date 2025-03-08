/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.village.TradeOffer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class QuickTrade extends Module {
    private final List<Supplier<Boolean>> tasks = Collections.synchronizedList(new LinkedList<>());
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Keybind> modifier = sgGeneral.add(new KeybindSetting.Builder()
        .name("Activation key")
        .description("Key to press to perform trade until exhausted.")
        .defaultValue(Keybind.none())
        .build()
    );

    public QuickTrade() {
        super(Categories.World, "quick-trade", "Quickly perform trades with villagers.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        synchronized (tasks) {
            tasks.removeIf(task -> !task.get());
        }
    }

    private void runUntilFalse(Supplier<Boolean> task) {
        tasks.add(task);
    }

    public void tradeUntilDoneOrEmpty(TradeOffer selectedOffer, MerchantScreenHandler handler, int selectedIndex) {
        final MinecraftClient client = MinecraftClient.getInstance();

        if (client.interactionManager == null) {
            error("Client interaction manager is null!");
            return;
        }

        Slot inSlot0 = handler.getSlot(0);
        Slot inSlot1 = handler.getSlot(1);
        Slot outputSlot = handler.getSlot(2);
        runUntilFalse(() -> {
            // If player is null or they changed screen
            if (client.player == null) {
                error("Player is null, stopping trading.");
                return false;
            }

            if (client.player.currentScreenHandler != handler) {
                error("Screen is closed, stopping trading.");
                return false;
            }

            if (client.getNetworkHandler() == null) {
                error("Network handler is null, stopping trading.");
                return false;
            }

            // If there is an item in the trade slot(s) already, then shift click or drop them
            if (!inSlot0.getStack().isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, inSlot0.id, 1, SlotActionType.QUICK_MOVE, client.player);
                client.interactionManager.clickSlot(handler.syncId, inSlot0.id, 1, SlotActionType.THROW, client.player);
            }

            if (!inSlot1.getStack().isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, inSlot1.id, 1, SlotActionType.QUICK_MOVE, client.player);
                client.interactionManager.clickSlot(handler.syncId, inSlot1.id, 1, SlotActionType.THROW, client.player);
            }

            // Refresh items
            handler.setRecipeIndex(selectedIndex);
            handler.switchTo(selectedIndex);

            client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(selectedIndex));

            // Out of materials OR trade is out of stock

            // todo auto-convert emerald blocks if we're out of them
            // todo auto-trade without clicking a trade (preset trades?)
            boolean shouldStopTrading = !selectedOffer.matchesBuyItems(
                handler.slots.get(0).getStack(),
                handler.slots.get(1).getStack())
                || selectedOffer.isDisabled();

            if (shouldStopTrading) {
                return false;
            }

            if (hasSpace(client.player.getInventory(), selectedOffer.getSellItem())) {
                client.interactionManager.clickSlot(handler.syncId, outputSlot.id, 0, SlotActionType.QUICK_MOVE, client.player);
            } else {
                client.interactionManager.clickSlot(handler.syncId, outputSlot.id, 0, SlotActionType.THROW, client.player);
            }

            return true;
        });
    }

    public boolean hasSpace(PlayerInventory inv, ItemStack outStack) {
        return outStack.isEmpty() || inv.getEmptySlot() >= 0 || inv.getOccupiedSlotWithRoomForStack(outStack) >= 0;
    }
}
