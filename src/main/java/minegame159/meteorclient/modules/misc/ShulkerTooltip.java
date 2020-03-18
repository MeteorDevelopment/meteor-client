package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;

public class ShulkerTooltip extends Module {
    private Setting<Integer> lines = addSetting(new IntSetting.Builder()
            .name("lines")
            .description("Number of lines.")
            .defaultValue(8)
            .min(0)
            .build()
    );

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

    public int lines() {
        return lines.get();
    }
}
