package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class ShulkerTooltip extends Module {
    public ShulkerTooltip() {
        super(Category.Misc, "shulker-tooltip", "Better shulker item tooltip.");
    }

    @Override
    public void onActivate() {
        MeteorClient.eventBus.post(EventStore.betterShulkerTooltipEvent(true));
    }

    @Override
    public void onDeactivate() {
        MeteorClient.eventBus.post(EventStore.betterShulkerTooltipEvent(false));
    }
}
