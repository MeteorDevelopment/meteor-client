package minegame159.meteorclient.modules.player;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class FastUse extends Module {
    public FastUse() {
        super(Category.Player, "fast-use", "Fast item use.");
    }

    @SubscribeEvent
    private void onTick(TickEvent e) {
        ((IMinecraftClient) mc).setItemUseCooldown(0);
    }
}
