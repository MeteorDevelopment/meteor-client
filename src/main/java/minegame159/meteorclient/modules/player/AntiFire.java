package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiFire extends Module {
    public AntiFire() {
        super(Category.Player, "anti-fire", "Removes fire.");
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> mc.player.extinguish());
}
