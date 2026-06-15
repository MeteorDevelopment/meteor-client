package meteordevelopment.meteorclient.utils.skyblock.terminal.sim;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.skyblock.TerminalSimulator;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalTypes;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TermStartGUI extends Screen {
    private static final TerminalTypes[] TYPES = TerminalTypes.values();

    public TermStartGUI() {
        super(Component.literal("Terminal Simulator"));
    }

    @Override
    protected void init() {
        int y = 40;
        for (TerminalTypes type : TYPES) {
            String label = switch (type) {
                case PANES -> "Correct all the panes!";
                case RUBIX -> "Change all to same color!";
                case NUMBERS -> "Click in order!";
                case STARTS_WITH -> "What starts with?";
                case SELECT -> "Select all the items!";
                case MELODY -> "Click the button on time!";
            };
            addRenderableWidget(Button.builder(Component.literal(label), btn -> openSim(type))
                .bounds(width / 2 - 100, y, 200, 20).build());
            y += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
            .bounds(width / 2 - 100, y + 10, 200, 20).build());
    }

    private void openSim(TerminalTypes type) {
        TermSimGUI gui = new TermSimGUI(type.windowSize, Component.literal(type.termName));
        gui.fillItems(SimUtils.generateItems(type));

        TerminalSimulator sim = Modules.get().get(TerminalSimulator.class);
        if (sim != null) {
            sim.onSimScreenOpen();
        }
        TerminalUtils.isSimulating = true;

        mc.setScreen(gui);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(mc.font, Component.literal("Terminal Simulator"), width / 2 - 50, 20, 0xFFFFFF, false);

        TerminalSimulator sim = Modules.get().get(TerminalSimulator.class);
        if (sim != null && sim.getPbTime() > 0) {
            graphics.text(mc.font, Component.literal("PB: " + sim.getPbTime() + "ms"), width / 2 - 50, 35, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE || mc.options.keyInventory.matches(input)) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }
}
