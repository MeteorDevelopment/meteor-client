package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class AutoSprint extends ToggleModule {
    public AutoSprint() {
        super(Category.Movement, "auto-sprint", "Automatically sprints.");
    }

    @Override
    public void onDeactivate() {
        mc.player.setSprinting(false);
    }

    @EventHandler
    private final Listener<TickEvent> onTick = new Listener<>(event -> {
        mc.player.setSprinting(true);
    });
}
