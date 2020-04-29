package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.EntityUtils;
import net.minecraft.entity.LivingEntity;

public class Chams extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private Setting<Boolean> players = sgGeneral.add(new BoolSetting.Builder()
            .name("players")
            .description("Render players.")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> animals = sgGeneral.add(new BoolSetting.Builder()
            .name("animals")
            .description("Render animals")
            .defaultValue(true)
            .build()
    );

    private Setting<Boolean> mobs = sgGeneral.add(new BoolSetting.Builder()
            .name("mobs")
            .description("Render mobs.")
            .defaultValue(true)
            .build()
    );

    public Chams() {
        super(Category.Render, "chams", "Renders entities through blocks.");
    }

    public boolean shouldRender(LivingEntity entity) {
        if (!isActive()) return false;
        if (EntityUtils.isPlayer(entity)) return players.get();
        if (EntityUtils.isAnimal(entity)) return animals.get();
        if (EntityUtils.isMob(entity)) return mobs.get();
        return false;
    }
}
