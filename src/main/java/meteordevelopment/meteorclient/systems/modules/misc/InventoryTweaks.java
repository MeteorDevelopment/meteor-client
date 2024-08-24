/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
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
import net.minecraft.item.BlockItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSorting = settings.createGroup("Sorting");
    private final SettingGroup sgAutoDrop = settings.createGroup("Auto Drop");
    private final SettingGroup sgStealDump = settings.createGroup("Steal and Dump");
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

    private final Setting<Boolean> autoDropExcludeEquipped = sgAutoDrop.add(new BoolSetting.Builder()
        .name("exclude-equipped")
        .description("Whether or not to drop items equipped in armor slots.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoDropExcludeHotbar = sgAutoDrop.add(new BoolSetting.Builder()
        .name("exclude-hotbar")
        .description("Whether or not to drop items from your hotbar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoDropOnlyFullStacks = sgAutoDrop.add(new BoolSetting.Builder()
        .name("only-full-stacks")
        .description("Only drops the items if the stack is full.")
        .defaultValue(false)
        .build()
    );

    // Steal & Dump

    public final Setting<List<ScreenHandlerType<?>>> stealScreens = sgStealDump.add(new ScreenHandlerListSetting.Builder()
        .name("steal-screens")
        .description("Select the screens to display buttons and auto steal.")
        .defaultValue(List.of(ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6))
        .build()
    );

    private final Setting<Boolean> buttons = sgStealDump.add(new BoolSetting.Builder()
        .name("inventory-buttons")
        .description("Shows steal and dump buttons in container guis.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> stealDrop = sgStealDump.add(new BoolSetting.Builder()
        .name("steal-drop")
        .description("Drop items to the ground instead of stealing them.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> dropBackwards = sgStealDump.add(new BoolSetting.Builder()
        .name("drop-backwards")
        .description("Drop items behind you.")
        .defaultValue(false)
        .visible(stealDrop::get)
        .build()
    );

    private final Setting<ListMode> dumpFilter = sgStealDump.add(new EnumSetting.Builder<ListMode>()
        .name("dump-filter")
        .description("Dump mode.")
        .defaultValue(ListMode.None)
        .build()
    );

    private final Setting<List<Item>> dumpItems = sgStealDump.add(new ItemListSetting.Builder()
        .name("dump-items")
        .description("Items to dump.")
        .build()
    );

    private final Setting<ListMode> stealFilter = sgStealDump.add(new EnumSetting.Builder<ListMode>()
        .name("steal-filter")
        .description("Steal mode.")
        .defaultValue(ListMode.None)
        .build()
    );

    private final Setting<List<Item>> stealItems = sgStealDump.add(new ItemListSetting.Builder()
        .name("steal-items")
        .description("Items to steal.")
        .build()
    );

    // Auto Steal

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

    private final Setting<Integer> autoStealInitDelay = sgAutoSteal.add(new IntSetting.Builder()
        .name("initial-delay")
        .description("The initial delay before stealing in milliseconds. 0 to use normal delay instead.")
        .defaultValue(50)
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

        if (sortingKey.get().matches(true, event.key, event.modifiers)) {
            if (sort()) event.cancel();
        }
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press) return;

        if (sortingKey.get().matches(false, event.button, 0)) {
            if (sort()) event.cancel();
        }
    }

    private boolean sort() {
        if (!sortingEnabled.get() || !(mc.currentScreen instanceof HandledScreen<?> screen) || sorter != null)
            return false;

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

    private boolean isWearable(ItemStack itemStack) {
        Item item = itemStack.getItem();

        if (item instanceof Equipment) return true;
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
                if ((!autoDropOnlyFullStacks.get() || itemStack.getCount() == itemStack.getMaxCount()) &&
                    !(autoDropExcludeEquipped.get() && SlotUtils.isArmor(i))) InvUtils.drop().slot(i);
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
            error("You can't enable Auto Steal and Auto Dump at the same time!");
            autoDump.set(false);
        }
    }

    private int getSleepTime() {
        return autoStealDelay.get() + (autoStealRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, autoStealRandomDelay.get()) : 0);
    }

    private void moveSlots(ScreenHandler handler, int start, int end, boolean steal) {
        boolean initial = autoStealInitDelay.get() != 0;
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;

            int sleep;
            if (initial) {
                sleep = autoStealInitDelay.get();
                initial = false;
            } else sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen or exit world
            if (mc.currentScreen == null || !Utils.canUpdate()) break;

            Item item = handler.getSlot(i).getStack().getItem();
            if (steal) {
                if (stealFilter.get() == ListMode.Whitelist && !stealItems.get().contains(item))
                    continue;
                if (stealFilter.get() == ListMode.Blacklist && stealItems.get().contains(item))
                    continue;
            } else {
                if (dumpFilter.get() == ListMode.Whitelist && !dumpItems.get().contains(item))
                    continue;
                if (dumpFilter.get() == ListMode.Blacklist && dumpItems.get().contains(item))
                    continue;
            }

            if (steal && stealDrop.get()) {
                if (dropBackwards.get()) {
                    int iCopy = i;
                    Rotations.rotate(mc.player.getYaw() - 180, mc.player.getPitch(), () -> InvUtils.drop().slotId(iCopy));
                }
            } else InvUtils.shiftClick().slotId(i);
        }
    }

    public void steal(ScreenHandler handler) {
        MeteorExecutor.execute(() -> moveSlots(handler, 0, SlotUtils.indexToId(SlotUtils.MAIN_START), true));
    }

    public void dump(ScreenHandler handler) {
        int playerInvOffset = SlotUtils.indexToId(SlotUtils.MAIN_START);
        MeteorExecutor.execute(() -> moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9, false));
    }

    public boolean showButtons() {
        return isActive() && buttons.get();
    }

    public boolean mouseDragItemMove() {
        return isActive() && mouseDragItemMove.get();
    }

    public boolean armorStorage() {
        return isActive() && armorStorage.get();
    }

    public boolean canSteal(ScreenHandler handler) {
        try {
            return (stealScreens.get().contains(handler.getType()));
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (canSteal(handler) && event.packet.getSyncId() == handler.syncId) {
            if (autoSteal.get()) {
                steal(handler);
            } else if (autoDump.get()) {
                dump(handler);
            }
        }
    }

    public enum ListMode {
        Whitelist,
        Blacklist,
        None
    }
}
