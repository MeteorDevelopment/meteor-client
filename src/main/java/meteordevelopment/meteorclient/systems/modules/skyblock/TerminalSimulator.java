package meteordevelopment.meteorclient.systems.modules.skyblock;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import meteordevelopment.meteorclient.utils.skyblock.terminal.sim.TermStartGUI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

public class TerminalSimulator extends Module {
    private boolean simScreenOpen = false;
    private long pbTime = 0;
    private long startTime = 0;
    private int clickCount = 0;

    public TerminalSimulator() {
        super(Categories.Skyblock, "terminal-simulator", "Practice terminals with simulated GUIs.");
    }

    @Override
    public void onActivate() {
        simScreenOpen = false;
        clickCount = 0;
        TerminalUtils.isSimulating = true;
        if (mc.screen == null) {
            mc.setScreen(new TermStartGUI());
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.screen instanceof meteordevelopment.meteorclient.utils.skyblock.terminal.sim.TermSimGUI
            || mc.screen instanceof TermStartGUI) {
            mc.setScreen(null);
        }
        simScreenOpen = false;
        TerminalUtils.isSimulating = false;
    }

    public void onSimScreenOpen() {
        simScreenOpen = true;
        startTime = System.currentTimeMillis();
        clickCount = 0;
    }

    public void onSimScreenClose() {
        if (simScreenOpen) {
            pbTime = System.currentTimeMillis() - startTime;
            simScreenOpen = false;
        }
    }

    public void onSimClick() {
        if (simScreenOpen) clickCount++;
    }

    public boolean isSimScreenOpen() {
        return simScreenOpen;
    }

    public long getPbTime() {
        return pbTime;
    }

    public int getClickCount() {
        return clickCount;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (simScreenOpen && event.packet instanceof ServerboundContainerClickPacket) {
            event.cancel();
        }
    }
}
