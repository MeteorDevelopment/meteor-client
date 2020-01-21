package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.ChamsEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
    private Setting<Boolean> players = addSetting(new BoolSettingBuilder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    public Chams() {
        super(Category.Render, "chams", "See entities through walls.");
    }

    @SubscribeEvent
    private void onChams(ChamsEvent e) {
        e.enabled = !players.value() || e.livingEntity instanceof PlayerEntity;
    }
}
