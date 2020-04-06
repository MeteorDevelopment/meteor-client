package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class Step extends ToggleModule {
    public Step() {
        super(Category.Movement, "step", "Allows you to step up full blocks.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        mc.player.stepHeight = 1;
    });
}
