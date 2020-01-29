package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderFogEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiFog extends Module {
    public AntiFog() {
        super(Category.Render, "anti-fog", "Disables fog.");
    }

    @EventHandler
    private Listener<RenderFogEvent> onRenderFog = new Listener<>(event -> event.cancel());
}
