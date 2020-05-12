package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.EntityUtils;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class ESP extends ToggleModule {
    public enum Mode {
        Lines,
        Sides,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgAnimals = settings.createGroup("Animals");
    private final SettingGroup sgMobs = settings.createGroup("Mobs");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgCrystals = settings.createGroup("Crystals");
    private final SettingGroup sgVehicles = settings.createGroup("Vehicles");

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Both)
            .build()
    );

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Color> othersColor = sgGeneral.add(new ColorSetting.Builder()
            .name("others-color")
            .description("Color of other entities.")
            .defaultValue(new Color(200, 200, 200))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> players = sgPlayers.add(new BoolSetting.Builder()
            .name("players")
            .description("See players.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> playersColor = sgPlayers.add(new ColorSetting.Builder()
            .name("players-color")
            .description("Players color.")
            .defaultValue(new Color(255, 255, 255, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> animals = sgAnimals.add(new BoolSetting.Builder()
            .name("animals")
            .description("See animals.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> animalsColor = sgAnimals.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(145, 255, 145, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> mobs = sgMobs.add(new BoolSetting.Builder()
            .name("mobs")
            .description("See mobs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> mobsColor = sgMobs.add(new ColorSetting.Builder()
            .name("mobs-color")
            .description("Mobs color.")
            .defaultValue(new Color(255, 145, 145, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> items = sgItems.add(new BoolSetting.Builder()
            .name("items")
            .description("See items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> itemsColor = sgItems.add(new ColorSetting.Builder()
            .name("items-color")
            .description("Items color.")
            .defaultValue(new Color(145, 145, 145, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> crystals = sgCrystals.add(new BoolSetting.Builder()
            .name("crystals")
            .description("See crystals.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> crystalsColor = sgCrystals.add(new ColorSetting.Builder()
            .name("crystals-color")
            .description("Crystals color.")
            .defaultValue(new Color(160, 40, 235, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Setting<Boolean> vehicles = sgVehicles.add(new BoolSetting.Builder()
            .name("vehicles")
            .description("See vehicles.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> vehiclesColor = sgVehicles.add(new ColorSetting.Builder()
            .name("vehicles-color")
            .description("Vehicles color.")
            .defaultValue(new Color(100, 100, 100, 255))
            .onChanged(color1 -> recalculateColor())
            .build()
    );

    private final Color playersLineColor = new Color();
    private final Color playersSideColor = new Color();
    private final Color animalsLineColor = new Color();
    private final Color animalsSideColor = new Color();
    private final Color mobsLineColor = new Color();
    private final Color mobsSideColor = new Color();
    private final Color itemsLineColor = new Color();
    private final Color itemsSideColor = new Color();
    private final Color crystalsLineColor = new Color();
    private final Color crystalsSideColor = new Color();
    private final Color vehiclesLineColor = new Color();
    private final Color vehiclesSideColor = new Color();
    private final Color othersLineColor = new Color();
    private final Color othersSideColor = new Color();
    private int count;

    public ESP() {
        super(Category.Render, "esp", "See entities through walls.");
        recalculateColor();
    }

    private void recalculateColor() {
        // Players
        playersLineColor.set(playersColor.get());

        playersSideColor.set(playersLineColor);
        playersSideColor.a -= 225;
        if (playersSideColor.a < 0) playersSideColor.a = 0;

        // Animals
        animalsLineColor.set(animalsColor.get());

        animalsSideColor.set(animalsLineColor);
        animalsSideColor.a -= 225;
        if (animalsSideColor.a < 0) animalsSideColor.a = 0;

        // Mobs
        mobsLineColor.set(mobsColor.get());

        mobsSideColor.set(mobsLineColor);
        mobsSideColor.a -= 225;
        if (mobsSideColor.a < 0) mobsSideColor.a = 0;

        // Items
        itemsLineColor.set(itemsColor.get());

        itemsSideColor.set(itemsLineColor);
        itemsSideColor.a -= 225;
        if (itemsSideColor.a < 0) itemsSideColor.a = 0;

        // Crystals
        crystalsLineColor.set(crystalsColor.get());

        crystalsSideColor.set(crystalsLineColor);
        crystalsSideColor.a -= 225;
        if (crystalsSideColor.a < 0) crystalsSideColor.a = 0;

        // Vehicles
        vehiclesLineColor.set(vehiclesColor.get());

        vehiclesSideColor.set(vehiclesLineColor);
        vehiclesSideColor.a -= 225;
        if (vehiclesSideColor.a < 0) vehiclesSideColor.a = 0;

        // Others
        othersLineColor.set(othersColor.get());

        othersSideColor.set(othersLineColor);
        othersSideColor.a -= 225;
        if (othersSideColor.a < 0) othersSideColor.a = 0;
    }

    private void render(Entity entity, Color lineColor, Color sideColor) {
        switch (mode.get()) {
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

        count++;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            if ((players.get() || entities.get().contains(entity.getType())) && EntityUtils.isPlayer(entity)) render(entity, playersLineColor, playersSideColor);
            else if ((animals.get() || entities.get().contains(entity.getType())) && EntityUtils.isAnimal(entity)) render(entity, animalsLineColor, animalsSideColor);
            else if ((mobs.get() || entities.get().contains(entity.getType())) && EntityUtils.isMob(entity)) render(entity, mobsLineColor, mobsSideColor);
            else if ((items.get() || entities.get().contains(entity.getType())) && EntityUtils.isItem(entity)) render(entity, itemsLineColor, itemsSideColor);
            else if ((crystals.get() || entities.get().contains(entity.getType())) && EntityUtils.isCrystal(entity)) render(entity, crystalsLineColor, crystalsSideColor);
            else if ((vehicles.get() || entities.get().contains(entity.getType())) && EntityUtils.isVehicle(entity)) render(entity, vehiclesLineColor, vehiclesSideColor);
            else if (entities.get().contains(entity.getType())) render(entity, othersLineColor, othersSideColor);
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
