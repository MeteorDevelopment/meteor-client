package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.KeyAction;

public class AirJump extends ToggleModule {
    public AirJump() {
        super(Category.Movement, "air-jump", "Lets you jump in air.");
    }

    @EventHandler
    private final Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (event.action == KeyAction.Press && mc.options.keyJump.matchesKey(event.key, 0)) {
            mc.player.jump();
        }
    });
}
