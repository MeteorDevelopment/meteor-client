package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.BoolSettingBuilder;
import minegame159.meteorclient.settings.builders.ColorSettingBuilder;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

public class ESP extends Module {
    public enum Mode {
        Lines,
        Sides,
        Both,
        Glowing
    }

    private Setting<Mode> mode = addSetting(new EnumSettingBuilder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Both)
            .build()
    );

    private Setting<Color> color = addSetting(new ColorSettingBuilder()
            .name("color")
            .description("Color.")
            .defaultValue(new Color(175, 175, 175, 255))
            .consumer((color1, color2) -> recalculateColor())
            .build()
    );

    private Setting<Boolean> players = addSetting(new BoolSettingBuilder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> animals = addSetting(new BoolSettingBuilder()
            .name("animals")
            .description("See animals.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> mobs = addSetting(new BoolSettingBuilder()
            .name("mobs")
            .description("See mobs.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> items = addSetting(new BoolSettingBuilder()
            .name("items")
            .description("See items.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> crystals = addSetting(new BoolSettingBuilder()
            .name("crystals")
            .description("See crystals.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> vehicles = addSetting(new BoolSettingBuilder()
            .name("vehicles")
            .description("See vehicles.")
            .defaultValue(true)
            .build()
    );

    private Color lineColor = new Color(0, 0, 0, 0);
    private Color sideColor = new Color(0, 0, 0, 0);

    public ESP() {
        super(Category.Render, "esp", "See entities through walls.");
        recalculateColor();
    }

    @Override
    public void onDeactivate() {
        if (mode.value() == Mode.Glowing) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity != mc.player) entity.setGlowing(false);
            }
        }
    }

    private void recalculateColor() {
        lineColor.set(color.value());

        sideColor.set(lineColor);
        sideColor.a -= 225;
        if (sideColor.a < 0) sideColor.a = 0;
    }

    private void render(Entity entity) {
        switch (mode.value()) {
            case Lines: {
                Box box = entity.getBoundingBox();
                RenderUtils.boxEdges(box.x1, box.y1, box.z1, box.x2, box.y2, box.z2, lineColor);
                break;
            }
            case Sides: {
                Box box = entity.getBoundingBox();
                RenderUtils.boxSides(box.x1, box.y1, box.z1, box.x2, box.y2, box.z2, sideColor);
                break;
            }
            case Both: {
                Box box = entity.getBoundingBox();
                RenderUtils.boxEdges(box.x1, box.y1, box.z1, box.x2, box.y2, box.z2, lineColor);
                RenderUtils.boxSides(box.x1, box.y1, box.z1, box.x2, box.y2, box.z2, sideColor);
                break;
            }
        }
    }

    @SubscribeEvent
    private void onRender(RenderEvent e) {
        for (Entity entity : mc.world.getEntities()) {
            if (players.value() && EntityUtils.isPlayer(entity) && entity != mc.player) render(entity);
            else if (animals.value() && EntityUtils.isAnimal(entity)) render(entity);
            else if (mobs.value() && EntityUtils.isMob(entity)) render(entity);
            else if (items.value() && EntityUtils.isItem(entity)) render(entity);
            else if (crystals.value() && EntityUtils.isCrystal(entity)) render(entity);
            else if (vehicles.value() && EntityUtils.isVehicle(entity)) render(entity);
        }
    }
}
