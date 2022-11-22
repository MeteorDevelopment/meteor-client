/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.CloseHandledScreenC2SPacketAccessor;
import meteordevelopment.meteorclient.mixin.HandledScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSorting = settings.createGroup("Sorting");
    private final SettingGroup sgAutoDrop = settings.createGroup("Auto Drop");
    private final SettingGroup sgAutoSteal = settings.createGroup("Auto Steal");

    // General

    private final Setting<Boolean> mouseDragItemMove = sgGeneral.add(new BoolSetting.Builder()
            .name("mouse-drag-item-move")
            .description("Moving mouse over items while holding shift will transfer it to the other container.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Item>> antiDropItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("anti-drop-items")
            .description("Items to prevent dropping. Doesn't work in creative inventory screen.")
            .build()
    );

    private final Setting<Boolean> buttons = sgGeneral.add(new BoolSetting.Builder()
        .name("inventory-buttons")
        .description("Shows steal and dump buttons in container guis.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> xCarry = sgGeneral.add(new BoolSetting.Builder()
        .name("xcarry")
        .description("Allows you to store four extra item stacks in your crafting grid.")
        .defaultValue(true)
        .onChanged(v -> {
            if (v || !Utils.canUpdate()) return;
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            invOpened = false;
        })
        .build()
    );

    private final Setting<Boolean> armorStorage = sgGeneral.add(new BoolSetting.Builder()
        .name("armor-storage")
        .description("Allows you to put normal items in your armor slots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> armorSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("armor-swap")
        .description("Lets you swap between pieces of armor by right clicking on it.")
        .defaultValue(true)
        .build()
    );

    // Sorting

    private final Setting<Boolean> sortingEnabled = sgSorting.add(new BoolSetting.Builder()
        .name("sorting-enabled")
        .description("Automatically sorts stacks in inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> sortingKey = sgSorting.add(new KeybindSetting.Builder()
        .name("sorting-key")
        .description("Key to trigger the sort.")
        .visible(sortingEnabled::get)
        .defaultValue(Keybind.fromButton(GLFW.GLFW_MOUSE_BUTTON_MIDDLE))
        .build()
    );

    private final Setting<Integer> sortingDelay = sgSorting.add(new IntSetting.Builder()
        .name("sorting-delay")
        .description("Delay in ticks between moving items when sorting.")
        .visible(sortingEnabled::get)
        .defaultValue(1)
        .min(0)
        .build()
    );

    // Auto Drop

    private final Setting<List<Item>> autoDropItems = sgAutoDrop.add(new ItemListSetting.Builder()
        .name("auto-drop-items")
        .description("Items to drop.")
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgAutoDrop.add(new BoolSetting.Builder()
        .name("auto-drop-exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDropOnlyFullStacks = sgAutoDrop.add(new BoolSetting.Builder()
        .name("auto-drop-only-full-stacks")
        .description("Only drops the items if the stack is full.")
        .defaultValue(false)
        .build()
    );

    // Auto steal

    private final Setting<Boolean> autoSteal = sgAutoSteal.add(new BoolSetting.Builder()
        .name("auto-steal")
        .description("Automatically removes all possible items when you open a container.")
        .defaultValue(false)
        .onChanged(val -> checkAutoStealSettings())
        .build()
    );

    private final Setting<Boolean> autoDump = sgAutoSteal.add(new BoolSetting.Builder()
        .name("auto-dump")
        .description("Automatically dumps all possible items when you open a container.")
        .defaultValue(false)
        .onChanged(val -> checkAutoStealSettings())
        .build()
    );

    private final Setting<Integer> autoStealDelay = sgAutoSteal.add(new IntSetting.Builder()
        .name("delay")
        .description("The minimum delay between stealing the next stack in milliseconds.")
        .defaultValue(20)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> autoStealRandomDelay = sgAutoSteal.add(new IntSetting.Builder()
        .name("random")
        .description("Randomly adds a delay of up to the specified time in milliseconds.")
        .min(0)
        .sliderMax(1000)
        .defaultValue(50)
        .build()
    );


    private InventorySorter sorter;
    private boolean invOpened;

    public InventoryTweaks() {
        super(Categories.Misc, "inventory-tweaks", "Various inventory related utilities.");
    }

    @Override
    public void onActivate() {
        invOpened = false;
    }

    @Override
    public void onDeactivate() {
        sorter = null;

        if (invOpened) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
        }
    }

    // Sorting and armour swapping

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;

        if (sortingKey.get().matches(true, event.key)) {
            if (sort()) event.cancel();
        }
        if (mc.options.useKey.matchesKey(event.key, 0) && armorSwap()) {
            if (swapArmor()) event.cancel();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press) return;

        if (sortingKey.get().matches(false, event.button)) {
            if (sort()) event.cancel();
        }
        if (mc.options.useKey.matchesMouse(event.button) && armorSwap()) {
            if (swapArmor()) event.cancel();
        }
    }

    private boolean sort() {
        if (!sortingEnabled.get() || !(mc.currentScreen instanceof HandledScreen<?> screen) || sorter != null) return false;

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            FindItemResult empty = InvUtils.findEmpty();
            if (!empty.found()) InvUtils.click().slot(-999);
            else InvUtils.click().slot(empty.slot());
        }

        Slot focusedSlot = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (focusedSlot == null) return false;

        sorter = new InventorySorter(screen, focusedSlot);
        return true;
    }

    private boolean swapArmor() {  // would mixin to use method in ArmorItem, but it's buggy and unreliable on servers
        if (mc.currentScreen != null) {
            if (!(mc.currentScreen instanceof InventoryScreen screen)) return false;
            Slot focusedSlot = ((HandledScreenAccessor) screen).getFocusedSlot();
            if (focusedSlot == null || !isWearable(focusedSlot.getStack())) return false;

            ItemStack itemStack = focusedSlot.getStack();
            EquipmentSlot equipmentSlot = LivingEntity.getPreferredEquipmentSlot(itemStack);

            //the way mojang handles the inventory is awful, and it took me too long to figure this out
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, SlotUtils.indexToId(focusedSlot.getIndex()),
                SlotUtils.ARMOR_START + equipmentSlot.getEntitySlotId(), SlotActionType.SWAP, mc.player);

        } else {
            ItemStack itemStack = mc.player.getMainHandStack();
            if (!isWearable(itemStack)) return false;
            EquipmentSlot equipmentSlot = LivingEntity.getPreferredEquipmentSlot(itemStack);

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, SlotUtils.indexToId(SlotUtils.ARMOR_START + (3 - equipmentSlot.getEntitySlotId())),
                mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);

        }
        return true;
    }

    private boolean isWearable(ItemStack itemStack) {
        Item item = itemStack.getItem();

        if (item instanceof Wearable) return true;
        return item instanceof BlockItem blockItem &&
            (blockItem.getBlock() instanceof AbstractSkullBlock || blockItem.getBlock() instanceof CarvedPumpkinBlock);
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        sorter = null;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (sorter != null && sorter.tick(sortingDelay.get())) sorter = null;
    }

    // Auto Drop

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Auto Drop
        if (mc.currentScreen instanceof HandledScreen<?> || autoDropItems.get().isEmpty()) return;

        for (int i = autoDropExcludeHotbar.get() ? 9 : 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (autoDropItems.get().contains(itemStack.getItem())) {
                if (!autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxCount()) InvUtils.drop().slot(i);
            }
        }
    }

    @EventHandler
    private void onDropItems(DropItemsEvent event) {
        if (antiDropItems.get().contains(event.itemStack.getItem())) event.cancel();
    }

    // XCarry

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!xCarry.get() || !(event.packet instanceof CloseHandledScreenC2SPacket)) return;

        if (((CloseHandledScreenC2SPacketAccessor) event.packet).getSyncId() == mc.player.playerScreenHandler.syncId) {
            invOpened = true;
            event.cancel();
        }
    }

    // Auto Steal

    private void checkAutoStealSettings() {
        if (autoSteal.get() && autoDump.get()) {
            ChatUtils.error("You can't enable Auto Steal and Auto Dump at the same time!");
            autoDump.set(false);
        }
    }

    private int getSleepTime() {
        return autoStealDelay.get() + (autoStealRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, autoStealRandomDelay.get()) : 0);
    }

    private int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    private void moveSlots(ScreenHandler handler, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;

            int sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen
            if (mc.currentScreen == null) break;

            InvUtils.quickMove().slotId(i);
        }
    }

    public void steal(ScreenHandler handler) {
        MeteorExecutor.execute(() -> moveSlots(handler, 0, getRows(handler) * 9));
    }

    public void dump(ScreenHandler handler) {
        int playerInvOffset = getRows(handler) * 9;
        MeteorExecutor.execute(() -> moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9));
    }

    public boolean showButtons() {
        return isActive() && buttons.get();
    }

    public boolean autoSteal() {
        return isActive() && autoSteal.get();
    }

    public boolean autoDump() {
        return isActive() && autoDump.get();
    }

    public boolean mouseDragItemMove() {
        return isActive() && mouseDragItemMove.get();
    }

    public boolean armorStorage() {
        return isActive() && armorStorage.get();
    }

    public boolean armorSwap() {
        return isActive() && armorSwap.get();
    }
}
