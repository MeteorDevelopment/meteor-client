package meteordevelopment.meteorclient.systems.modules.skyblock;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.skyblock.terminal.TerminalUtils;
import meteordevelopment.meteorclient.utils.skyblock.terminal.handlers.TerminalHandler;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.ContainerInput;

import java.util.*;
import java.util.stream.Collectors;

public class AutoTerms extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minDelay = sgGeneral.add(new IntSetting.Builder()
        .name("min-delay").description("Minimum delay between clicks (ms).")
        .defaultValue(80).range(0, 500).sliderRange(0, 500).build()
    );

    private final Setting<Integer> maxDelay = sgGeneral.add(new IntSetting.Builder()
        .name("max-delay").description("Maximum delay between clicks (ms).")
        .defaultValue(160).range(0, 500).sliderRange(0, 500).build()
    );

    private final Setting<Integer> firstClickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("first-click-delay").description("Delay before first click after opening terminal (ms).")
        .defaultValue(350).range(0, 1000).sliderRange(0, 1000).build()
    );

    public enum OrderMode {
        First,
        Random,
        Closest,
        Furthest
    }

    private final Setting<OrderMode> order = sgGeneral.add(new EnumSetting.Builder<OrderMode>()
        .name("order").description("Order to click slots.")
        .defaultValue(OrderMode.Closest).build()
    );

    private final Setting<Boolean> numbers = sgGeneral.add(new BoolSetting.Builder()
        .name("numbers").description("Solve numbers terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> panes = sgGeneral.add(new BoolSetting.Builder()
        .name("panes").description("Solve panes terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> colors = sgGeneral.add(new BoolSetting.Builder()
        .name("colors").description("Solve select color terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> name = sgGeneral.add(new BoolSetting.Builder()
        .name("name").description("Solve starts with terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> rubix = sgGeneral.add(new BoolSetting.Builder()
        .name("rubix").description("Solve rubix terminals.")
        .defaultValue(true).build()
    );

    private final Setting<Boolean> melody = sgGeneral.add(new BoolSetting.Builder()
        .name("melody").description("Solve melody terminals.")
        .defaultValue(true).build()
    );

    private final Random rng = new Random();
    private final Queue<Integer> clickQueue = new LinkedList<>();
    private int lastSlot = -1;
    private long nextClickTime = 0;
    private int terminalId = -1;

    public AutoTerms() {
        super(Categories.Skyblock, "auto-terms", "Automatically solves terminals!");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        reset();
        nextClickTime = System.currentTimeMillis() + firstClickDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;

        TerminalHandler term = TerminalUtils.getCurrentTerm();
        if (term == null) return;

        String typeName = term.type.name();
        boolean isMelody = typeName.equals("MELODY");

        if (isMelody) {
            if (melody.get()) handleMelody(term);
            return;
        }

        if (!isTypeEnabled(typeName)) return;
        if (term.solution.isEmpty()) return;

        if (System.currentTimeMillis() < nextClickTime) return;

        if (!clickQueue.isEmpty()) {
            int slot = clickQueue.poll();
            if (!term.solution.contains(slot)) return;
            int button = term.getClickButton(slot);
            executeClick(term, slot, button);
            scheduleNextClick();
        } else if (typeName.equals("NUMBERS")) {
            int slot = term.solution.get(0);
            if (!term.solution.contains(slot)) return;
            if (slot == lastSlot) return;
            lastSlot = slot;
            int button = term.getClickButton(slot);
            executeClick(term, slot, button);
            scheduleNextClick();
        } else {
            List<Integer> clicks = new ArrayList<>(term.solution);
            Integer pick = pick(clicks, term);
            if (pick == null) return;
            if (pick == lastSlot) return;
            lastSlot = pick;
            int button = term.getClickButton(pick);
            executeClick(term, pick, button);
            scheduleNextClick();
        }
    }

    private void handleMelody(TerminalHandler term) {
        if (System.currentTimeMillis() < nextClickTime) return;

        for (int s : term.solution) {
            if (s % 9 == 7) {
                executeClick(term, s, 0);
                scheduleNextClick();
                return;
            }
        }
    }

    private boolean isTypeEnabled(String typeName) {
        return switch (typeName) {
            case "NUMBERS" -> numbers.get();
            case "PANES" -> panes.get();
            case "SELECT" -> colors.get();
            case "STARTS_WITH" -> name.get();
            case "RUBIX" -> rubix.get();
            case "MELODY" -> melody.get();
            default -> false;
        };
    }

    private void scheduleNextClick() {
        long delay = minDelay.get() >= maxDelay.get()
            ? minDelay.get()
            : minDelay.get() + (long) (rng.nextDouble() * (maxDelay.get() - minDelay.get()));
        nextClickTime = System.currentTimeMillis() + delay;
    }

    private Integer pick(List<Integer> clicks, TerminalHandler term) {
        if (clicks.isEmpty()) return null;
        return switch (order.get()) {
            case First -> clicks.get(0);
            case Random -> clicks.get(rng.nextInt(clicks.size()));
            case Closest -> clicks.stream().min(Comparator.comparingDouble(s -> dist(s, lastSlot >= 0 ? lastSlot : s))).orElse(null);
            case Furthest -> clicks.stream().max(Comparator.comparingDouble(s -> dist(s, lastSlot >= 0 ? lastSlot : s))).orElse(null);
        };
    }

    private double dist(int a, int b) {
        if (mc.player == null) return Double.MAX_VALUE;
        var menu = mc.player.containerMenu;
        if (a < 0 || a >= menu.slots.size() || b < 0 || b >= menu.slots.size()) return Double.MAX_VALUE;

        var slotA = menu.getSlot(a);
        var slotB = menu.getSlot(b);
        double dx = slotA.x - slotB.x;
        double dy = slotA.y - slotB.y;
        double d = Math.sqrt(dx * dx + dy * dy);
        return d + rng.nextGaussian() * (d * 0.25);
    }

    private void executeClick(TerminalHandler term, int slot, int button) {
        if (mc.player == null) return;
        var menu = mc.player.containerMenu;
        mc.gameMode.handleContainerInput(menu.containerId, slot, 2, ContainerInput.CLONE, mc.player);
        lastSlot = slot;
    }

    private void reset() {
        clickQueue.clear();
        lastSlot = -1;
        nextClickTime = 0;
        terminalId = -1;
    }
}
