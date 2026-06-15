package meteordevelopment.meteorclient.utils.skyblock.terminal;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.ContainerSlotUpdateEvent;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Pattern CHAT_TERMINAL_PATTERN = Pattern.compile("^(.{1,16}) activated a terminal! \\((\\d)/(\\d)\\)$");

    public static boolean isSimulating = false;

    private static TerminalHandler currentTerm = null;
    private static TerminalHandler lastTermOpened = null;
    private static long lastClickTime = 0L;

    public static TerminalHandler getCurrentTerm() {
        return currentTerm;
    }

    public static TerminalHandler getLastTermOpened() {
        return lastTermOpened;
    }

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(new TerminalUtils());
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AbstractContainerScreen<?> containerScreen) {
            String title = containerScreen.getTitle().getString();
            TerminalTypes type = null;
            for (TerminalTypes t : TerminalTypes.values()) {
                if (t.regex.matcher(title).matches()) {
                    type = t;
                    break;
                }
            }
            if (type != null) {
                if (currentTerm != null && !currentTerm.isClicked && currentTerm.windowCount <= 2) {
                    leaveTerm();
                }
                TerminalHandler handler = type.createHandler(title);
                if (handler != null) {
                    currentTerm = handler;
                    MeteorClient.EVENT_BUS.post(TerminalEvent.Open.get(handler));
                    lastTermOpened = handler;
                }
                handler.openScreen();

                if (isSimulating) {
                    populateHandlerFromContainer(containerScreen, type.windowSize);
                }
            }
        }
    }

    public static void populateHandlerFromContainer(AbstractContainerScreen<?> containerScreen, int windowSize) {
        if (currentTerm == null) return;
        var menu = containerScreen.getMenu();
        if (menu != null && menu.slots.size() >= windowSize) {
            var items = menu.slots.subList(0, windowSize).stream().map(s -> s.getItem()).toList();
            for (int i = 0; i < items.size(); i++) {
                currentTerm.updateSlot(items, i);
            }
        }
    }

    @EventHandler
    private void onContainerSlotUpdate(ContainerSlotUpdateEvent event) {
        if (isSimulating) return;
        if (currentTerm == null) return;
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            ClientboundContainerSetSlotPacket packet = event.packet;
            int slot = packet.getSlot();
            if (slot >= 0 && slot < currentTerm.type.windowSize) {
                var menu = containerScreen.getMenu();
                if (menu.slots.size() >= currentTerm.type.windowSize) {
                    currentTerm.updateSlot(menu.slots.subList(0, currentTerm.type.windowSize).stream().map(s -> s.getItem()).toList(), slot);
                }
            }
        }
    }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        if (isSimulating) return;
        if (currentTerm == null) return;
        if (event.packet.containerId() == (mc.player != null ? mc.player.containerMenu.containerId : -1)) return;
        if (mc.screen instanceof AbstractContainerScreen<?>) {
            currentTerm.isClicked = false;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isSimulating) return;
        if (currentTerm == null) return;
        if (mc.screen == null || !(mc.screen instanceof AbstractContainerScreen)) {
            leaveTerm();
        }
    }

    private static void leaveTerm() {
        if (currentTerm != null) {
            MeteorClient.EVENT_BUS.post(TerminalEvent.Close.get(currentTerm));
            currentTerm = null;
        }
    }

    public static void handleSlotClick(int slotId, int button) {
        lastClickTime = System.currentTimeMillis();
        if (currentTerm != null) currentTerm.isClicked = true;
    }

    public static void handleChatMessage(String message) {
        Matcher matcher = CHAT_TERMINAL_PATTERN.matcher(message);
        if (matcher.matches() && lastTermOpened != null) {
            String playerName = message.split(" ")[0];
            if (mc.player != null && playerName.equals(mc.player.getName().getString())) {
                MeteorClient.EVENT_BUS.post(TerminalEvent.Solve.get(lastTermOpened));
            }
        }
    }
}
