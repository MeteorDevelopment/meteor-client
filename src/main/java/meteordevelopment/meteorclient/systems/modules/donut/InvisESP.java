package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;

public class InvisESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of invisible players.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .build()
    );

    public InvisESP() {
        super(Categories.Donut, "invis-esp", "Shows invisible players.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null) return;

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isInvisible()) {
                event.renderer.box(player.getBoundingBox(), color.get(), color.get(), ShapeMode.Both, 0);
            }
        }
    }
}
