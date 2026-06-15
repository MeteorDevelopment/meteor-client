package meteordevelopment.meteorclient.systems.modules.skyblock;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.mixin.AbstractContainerScreenAccessor;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class TerminalSolver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFunctionality = settings.createGroup("Functionality");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    public enum RenderMode {
        Odin,
        Normal,
        CustomGUI
    }

    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode").description("How the terminal solver should render.")
        .defaultValue(RenderMode.Normal).build()
    );

    private final Setting<Integer> normalTermSize = sgGeneral.add(new IntSetting.Builder()
        .name("normal-term-size").description("GUI scale increase for normal terminal GUI.")
        .defaultValue(3).range(1, 5).sliderRange(1, 5).build()
    );

    private final Setting<Double> customTermSize = sgGeneral.add(new DoubleSetting.Builder()
        .name("term-size").description("Size of the custom terminal GUI.")
        .defaultValue(2.0).range(1.0, 3.0).sliderRange(1.0, 3.0).build()
    );

    private final Setting<Double> roundness = sgGeneral.add(new DoubleSetting.Builder()
        .name("roundness").description("Roundness of the custom terminal GUI.")
        .defaultValue(5.0).range(0.0, 15.0).sliderRange(0.0, 15.0).build()
    );

    private final Setting<Integer> gap = sgGeneral.add(new IntSetting.Builder()
        .name("slot-gap").description("Gap between slots in custom terminal GUI.")
        .defaultValue(2).range(0, 8).sliderRange(0, 8).build()
    );

    private final Setting<Boolean> cancelTooltips = sgFunctionality.add(new BoolSetting.Builder()
        .name("stop-tooltips").description("Stops rendering tooltips in terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> middleClickGUI = sgFunctionality.add(new BoolSetting.Builder()
        .name("middle-click-gui").description("Replaces right click with middle click in terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> blockIncorrectClicks = sgFunctionality.add(new BoolSetting.Builder()
        .name("block-incorrect-clicks").description("Blocks incorrect clicks in terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> cancelMelody = sgFunctionality.add(new BoolSetting.Builder()
        .name("stop-melody-solver").description("Stops rendering the melody solver.")
        .defaultValue(false).build()
    );

    private final Setting<Double> melodyTermSize = sgFunctionality.add(new DoubleSetting.Builder()
        .name("melody-size").description("Size of the melody terminal GUI.")
        .defaultValue(1.5).range(1.0, 3.0).sliderRange(1.0, 3.0)
        .visible(() -> !cancelMelody.get()).build()
    );

    private final Setting<Boolean> showNumbers = sgFunctionality.add(new BoolSetting.Builder()
        .name("show-numbers").description("Shows numbers in the order terminal.")
        .defaultValue(true).build()
    );

    private final Setting<Integer> firstClickProt = sgFunctionality.add(new IntSetting.Builder()
        .name("first-click-protection").description("Time after opening a terminal where clicks are blocked (ms).")
        .defaultValue(500).range(350, 800).sliderRange(350, 800).build()
    );

    private final Setting<Boolean> hideClicked = sgFunctionality.add(new BoolSetting.Builder()
        .name("hide-clicked").description("Visually hides first click before GUI updates.")
        .defaultValue(false).build()
    );

    private final Setting<Integer> reloadThreshold = sgFunctionality.add(new IntSetting.Builder()
        .name("resolve-timeout").description("Time before terminal reloads after unregistered click (ms).")
        .defaultValue(600).range(300, 1000).sliderRange(300, 1000)
        .visible(() -> hideClicked.get()).build()
    );

    private final Setting<Boolean> debug = sgFunctionality.add(new BoolSetting.Builder()
        .name("debug").description("Shows debug terminal info.")
        .defaultValue(false).build()
    );

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background").description("Background color.")
        .defaultValue(new SettingColor(66, 66, 66)).build()
    );

    private final Setting<SettingColor> panesColor = sgColors.add(new ColorSetting.Builder()
        .name("panes").description("Color for panes terminal.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> rubixColor1 = sgColors.add(new ColorSetting.Builder()
        .name("rubix-1").description("Color for 1 click on rubix.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> rubixColor2 = sgColors.add(new ColorSetting.Builder()
        .name("rubix-2").description("Color for 2 clicks on rubix.")
        .defaultValue(new SettingColor(0, 200, 0)).build()
    );

    private final Setting<SettingColor> oppositeRubixColor1 = sgColors.add(new ColorSetting.Builder()
        .name("opposite-rubix-1").description("Color for -1 click on rubix.")
        .defaultValue(new SettingColor(200, 0, 0)).build()
    );

    private final Setting<SettingColor> oppositeRubixColor2 = sgColors.add(new ColorSetting.Builder()
        .name("opposite-rubix-2").description("Color for -2 clicks on rubix.")
        .defaultValue(new SettingColor(150, 0, 0)).build()
    );

    private final Setting<SettingColor> orderColor1 = sgColors.add(new ColorSetting.Builder()
        .name("order-1").description("Color for 1st item in order terminal.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> orderColor2 = sgColors.add(new ColorSetting.Builder()
        .name("order-2").description("Color for 2nd item in order terminal.")
        .defaultValue(new SettingColor(0, 200, 0)).build()
    );

    private final Setting<SettingColor> orderColor3 = sgColors.add(new ColorSetting.Builder()
        .name("order-3").description("Color for 3rd item in order terminal.")
        .defaultValue(new SettingColor(0, 150, 0)).build()
    );

    private final Setting<SettingColor> startsWithColor = sgColors.add(new ColorSetting.Builder()
        .name("starts-with").description("Color for starts with terminal.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> selectColor = sgColors.add(new ColorSetting.Builder()
        .name("select").description("Color for select terminal.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> melodyColumnColor = sgColors.add(new ColorSetting.Builder()
        .name("melody-column").description("Color for melody column indicator.")
        .defaultValue(new SettingColor(128, 0, 128)).build()
    );

    private final Setting<SettingColor> melodyPointerColor = sgColors.add(new ColorSetting.Builder()
        .name("melody-pointer").description("Color for melody pointer.")
        .defaultValue(new SettingColor(0, 255, 0)).build()
    );

    private final Setting<SettingColor> melodyBackgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("melody-background").description("Color for melody background slots.")
        .defaultValue(new SettingColor(97, 97, 97)).build()
    );

    private AbstractContainerScreen<?> currentScreen;

    public TerminalSolver() {
        super(Categories.Skyblock, "terminal-solver", "Renders solutions for dungeon terminals.");
    }

    @Override
    public void onActivate() {
        currentScreen = null;
    }

    @Override
    public void onDeactivate() {
        currentScreen = null;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AbstractContainerScreen<?> containerScreen) {
            currentScreen = containerScreen;
        } else {
            currentScreen = null;
        }
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term == null || !isActive()) return;

        if (cancelMelody.get() && term.type.name().equals("MELODY")) return;

        if (currentScreen == null) return;

        if (renderMode.get() == RenderMode.CustomGUI) {
            renderCustomGui(event, term);
            if (debug.get()) renderDebug(event, term);
            return;
        }

        if (currentScreen instanceof AbstractContainerScreenAccessor accessor) {
            int leftPos = accessor.meteor$getLeftPos();
            int topPos = accessor.meteor$getTopPos();

            for (int i = 0; i < term.type.windowSize; i++) {
                Color color = term.getSlotColor(i);
                if (color == null) continue;

                int row = i / 9;
                int col = i % 9;
                int x = leftPos + col * 18 + 7;
                int y = topPos + row * 18 + 17;

                event.graphics.fill(x, y, x + 16, y + 16, color.getPacked());

                String text = term.getSlotText(i);
                if (text != null && showNumbers.get()) {
                    VanillaTextRenderer.INSTANCE.begin(1.0, false, false);
                    VanillaTextRenderer.INSTANCE.render(text, x + 4, y + 2, Color.WHITE, false);
                    VanillaTextRenderer.INSTANCE.end();
                }
            }
        }

        if (debug.get()) renderDebug(event, term);
    }

    private void renderCustomGui(Render2DEvent event, TerminalHandler term) {
        int windowSize = term.type.windowSize;
        int rows = windowSize / 9;
        int cols = 9;

        double scale = customTermSize.get();
        int slotSize = (int) Math.round(18 * scale);
        int slotGap = gap.get();

        int gridWidth = cols * slotSize + (cols - 1) * slotGap;
        int gridHeight = rows * slotSize + (rows - 1) * slotGap;

        int originX = (event.screenWidth - gridWidth) / 2;
        int originY = (event.screenHeight - gridHeight) / 2;

        int padding = 8;
        event.graphics.fill(originX - padding, originY - padding, originX + gridWidth + padding, originY + gridHeight + padding, backgroundColor.get().getPacked());

        for (int i = 0; i < windowSize; i++) {
            int row = i / 9;
            int col = i % 9;
            int x = originX + col * (slotSize + slotGap);
            int y = originY + row * (slotSize + slotGap);

            event.graphics.fill(x, y, x + slotSize, y + slotSize, new Color(60, 60, 60).getPacked());

            Color color = term.getSlotColor(i);
            if (color != null) {
                event.graphics.fill(x, y, x + slotSize, y + slotSize, color.getPacked());
            }

            String text = term.getSlotText(i);
            if (text != null && showNumbers.get()) {
                VanillaTextRenderer.INSTANCE.begin(1.0, false, false);
                VanillaTextRenderer.INSTANCE.render(text, x + 4, y + 2, Color.WHITE, false);
                VanillaTextRenderer.INSTANCE.end();
            }
        }
    }

    private void renderDebug(Render2DEvent event, TerminalHandler term) {
        String[] info = {
            "Type: " + term.type.name(),
            "Time Open: " + (System.currentTimeMillis() - term.timeOpened) + "ms",
            "Clicked: " + term.isClicked,
            "Window Count: " + term.windowCount,
            "Solution: " + term.solution.toString()
        };

        int y = 20;
        for (String line : info) {
            VanillaTextRenderer.INSTANCE.begin(1.0, false, false);
            VanillaTextRenderer.INSTANCE.render(line, 5, y, Color.WHITE, false);
            VanillaTextRenderer.INSTANCE.end();
            y += 10;
        }
    }

    public boolean canCancelTooltips() {
        return isActive() && cancelTooltips.get() && TerminalUtils.getCurrentTerm() != null;
    }

    public boolean shouldInterceptClick(int slotId, int button) {
        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term == null || !isActive()) return false;
        if (System.currentTimeMillis() - term.timeOpened < firstClickProt.get()) return true;
        return blockIncorrectClicks.get() && !term.canClick(slotId, button);
    }

    public void handleClick(int slotId, int button) {
        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term == null || !isActive()) return;

        if (middleClickGUI.get()) {
            int btn = button == 0 ? 2 : button;
            term.click(slotId, btn, hideClicked.get() && !term.isClicked);
        } else if (hideClicked.get() && !term.isClicked) {
            term.simulateClick(slotId, button);
        }
    }
}
