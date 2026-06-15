package meteordevelopment.meteorclient.systems.modules.skyblock;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;
import meteordevelopment.orbit.EventHandler;

public class TerminalReopener extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minClicks = sgGeneral.add(new IntSetting.Builder()
        .name("min-clicks").description("Minimum clicks required. Reopens terminal if solution needs fewer.")
        .defaultValue(5).range(1, 20).sliderRange(1, 20).build()
    );

    private final Setting<Integer> reopenDelay = sgGeneral.add(new IntSetting.Builder()
        .name("reopen-delay").description("Delay before closing terminal (ms).")
        .defaultValue(300).range(0, 1000).sliderRange(0, 1000).build()
    );

    private final Setting<Boolean> rubix = sgGeneral.add(new BoolSetting.Builder()
        .name("rubix").description("Reopen rubix terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> numbers = sgGeneral.add(new BoolSetting.Builder()
        .name("numbers").description("Reopen numbers terminals.")
        .defaultValue(false).build()
    );

    private final Setting<Boolean> panes = sgGeneral.add(new BoolSetting.Builder()
        .name("panes").description("Reopen panes terminals.")
        .defaultValue(false).build()
    );

    private final Setting<Boolean> colors = sgGeneral.add(new BoolSetting.Builder()
        .name("colors").description("Reopen select color terminals.")
        .defaultValue(false).build()
    );

    private final Setting<Boolean> name = sgGeneral.add(new BoolSetting.Builder()
        .name("name").description("Reopen starts with terminals.")
        .defaultValue(false).build()
    );

    public TerminalReopener() {
        super(Categories.Skyblock, "terminal-reopener", "Automatically reopens terminals that require too few clicks.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;

        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term == null) return;
        if (term.solution.isEmpty()) return;

        if (System.currentTimeMillis() - term.timeOpened < reopenDelay.get()) return;

        String typeName = term.type.name();
        if (!isTypeEnabled(typeName)) return;

        if (term.solution.size() < minClicks.get()) {
            if (mc.player != null) {
                mc.player.closeContainer();
            }
        }
    }

    private boolean isTypeEnabled(String typeName) {
        return switch (typeName) {
            case "RUBIX" -> rubix.get();
            case "NUMBERS" -> numbers.get();
            case "PANES" -> panes.get();
            case "SELECT" -> colors.get();
            case "STARTS_WITH" -> name.get();
            default -> false;
        };
    }
}
