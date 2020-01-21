package minegame159.meteorclient.modules.render;

import com.google.common.collect.Iterables;
import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.Render2DEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Reflection;
import minegame159.meteorclient.utils.Utils;

public class Info extends Module {
    private Setting<Boolean> fps = addSetting(new BoolSettingBuilder()
            .name("fps")
            .description("Display fps.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> entities = addSetting(new BoolSettingBuilder()
            .name("entities")
            .description("Display number of entities.")
            .defaultValue(true)
            .build()
    );

    public Info() {
        super(Category.Render, "info", "Displays various info.");
    }

    private void drawInfo(String text1, String text2, int y) {
        Utils.drawText(text1, 2, y, Color.fromRGBA(255, 255, 255, 255));
        Utils.drawText(text2, 2 + Utils.getTextWidth(text1), y, Color.fromRGBA(185, 185, 185, 255));
    }

    @SubscribeEvent
    private void onRender2D(Render2DEvent e) {
        int y = 2;

        if (fps.value()) {
            drawInfo("FPS: ", Reflection.MinecraftClient_currentFps.get(mc) + "", y);
            y += Utils.getTextHeight() + 2;
        }

        if (entities.value()) {
            drawInfo("No Entities: ", Iterables.size(mc.world.getEntities()) + "", y);
            y += Utils.getTextHeight() + 2;
        }
    }
}
