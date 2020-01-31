package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super(Category.Player, "auto-respawn", "Automatically respawns.");
    }

    @EventHandler
    private Listener<OpenScreenEvent> onOpenScreenEvent = new Listener<>(event -> {
        if (!(event.screen instanceof DeathScreen)) return;

        mc.player.requestRespawn();
        event.cancel();
    });
}
