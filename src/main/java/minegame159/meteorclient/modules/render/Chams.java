/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class Chams extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Select entities to show through walls.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    public Chams() {
        super(Category.Render, "chams", "Renders entities through blocks.");
    }

    public boolean shouldRender(Entity entity) {
        return isActive() && entities.get().contains(entity.getType());
    }
}
