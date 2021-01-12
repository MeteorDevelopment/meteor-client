package minegame159.meteorclient.modules.combat;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class Hitboxes extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Which entities to target.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Double> value = sgGeneral.add(new DoubleSetting.Builder()
            .name("value")
            .description("How much to expand the hitboxes.")
            .defaultValue(0.5)
            .build()
    );

    public Hitboxes() {
        super(Category.Combat, "hitboxes", "Expands entity hitboxes.");
    }

    public double getEntityValue(Entity entity) {
        if (!isActive()) return 0;
        if (entities.get().contains(entity.getType())) return value.get();
        return 0;
    }
}
