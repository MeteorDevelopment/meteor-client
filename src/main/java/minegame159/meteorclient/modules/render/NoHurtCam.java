package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.HurtCamEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super(Category.Render, "no-hurt-cam", "Disables hurt camera effect.");
    }

    @SubscribeEvent
    private void onHurtCam(HurtCamEvent e) {
        e.setCancelled(true);
    }
}
