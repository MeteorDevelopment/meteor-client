package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.ChamsEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
    private static BoolSetting players = new BoolSetting("players", "See players.", true);

    public Chams() {
        super(Category.Render, "chams", "See entities through walls.", players);
    }

    @SubscribeEvent
    private void onChams(ChamsEvent e) {
        e.enabled = !players.value || e.livingEntity instanceof PlayerEntity;
    }
}
