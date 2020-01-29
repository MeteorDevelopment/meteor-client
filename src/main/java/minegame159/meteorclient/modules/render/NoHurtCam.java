package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.HurtCamEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super(Category.Render, "no-hurt-cam", "Disables hurt camera effect.");
    }

    @EventHandler
    private Listener<HurtCamEvent> onHurtCam = new Listener<>(event -> event.cancel());
}
