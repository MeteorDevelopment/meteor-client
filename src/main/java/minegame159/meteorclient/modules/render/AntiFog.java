package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.RenderFogEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiFog extends Module {
    public AntiFog() {
        super(Category.Render, "anti-fog", "Disables fog.");
    }

    @SubscribeEvent
    private void onRenderFog(RenderFogEvent e) {
        e.setCancelled(true);
    }
}
