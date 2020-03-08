package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TookDamageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DeathPosition extends Module {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public DeathPosition() {
        super(Category.Player, "death-position", "Sends to your chat where you died.");
    }

    @EventHandler
    private Listener<TookDamageEvent> onTookDamage = new Listener<>(event -> {
        if (event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() <= 0) {
            Utils.sendMessage("#yellowDied at #blue%.1f#yellow, #blue%.1f#yellow, #blue%.1f#yellow on #blue%s.", mc.player.x, mc.player.y, mc.player.z, dateFormat.format(new Date()));
        }
    });
}
