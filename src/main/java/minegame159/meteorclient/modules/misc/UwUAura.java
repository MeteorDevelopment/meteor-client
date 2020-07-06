package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.entity.player.PlayerEntity;

public class UwUAura extends ToggleModule {
    public UwUAura() {
        super(Category.Misc, "UwU-aura", "Sends UwU to every player when they enter render distance. UwU.");
    }

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (!(event.entity instanceof PlayerEntity)) return;

        mc.player.sendChatMessage("/msg " + ((PlayerEntity) event.entity).getGameProfile().getName() + " UwU");
    });
}
