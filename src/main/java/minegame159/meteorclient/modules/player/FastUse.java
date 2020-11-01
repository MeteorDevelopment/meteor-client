package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;

public class FastUse extends ToggleModule {
    public FastUse() {
        super(Category.Player, "fast-use", "Fast item use.");
    }

    @EventHandler
    private Listener<PostTickEvent> onTick = new Listener<>(event -> ((IMinecraftClient) mc).setItemUseCooldown(0));
}
