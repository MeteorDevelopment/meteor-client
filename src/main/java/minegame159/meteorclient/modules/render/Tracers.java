/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> storage = sgGeneral.add(new BoolSetting.Builder()
            .name("storage")
            .description("Displays storage blocks.")
            .defaultValue(false)
            .build()
    );

    // Appearance

    private final Setting<RenderUtils.TracerTarget> target = sgAppearance.add(new EnumSetting.Builder<RenderUtils.TracerTarget>()
            .name("target")
            .description("Which body part to target.")
            .defaultValue(RenderUtils.TracerTarget.Body)
            .build()
    );

    private final Setting<Boolean> stem = sgAppearance.add(new BoolSetting.Builder()
            .name("stem")
            .description("Draw a line through the center of the tracer target.")
            .defaultValue(true)
            .build()
    );

    // Colors

    private final Setting<Color> playersColor = sgColors.add(new ColorSetting.Builder()
            .name("players-colors")
            .description("The player's color.")
            .defaultValue(new Color(205, 205, 205, 127))
            .build()
    );

    private final Setting<Color> animalsColor = sgColors.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("The animal's color.")
            .defaultValue(new Color(145, 255, 145, 127))
            .build()
    );

    private final Setting<Color> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
            .name("water-animals-color")
            .description("The water animal's color.")
            .defaultValue(new Color(145, 145, 255, 127))
            .build()
    );

    private final Setting<Color> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("The monster's color.")
            .defaultValue(new Color(255, 145, 145, 127))
            .build()
    );

    private final Setting<Color> ambientColor = sgColors.add(new ColorSetting.Builder()
            .name("ambient-color")
            .description("The ambient color.")
            .defaultValue(new Color(75, 75, 75, 127))
            .build()
    );

    private final Setting<Color> miscColor = sgColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("The misc color.")
            .defaultValue(new Color(145, 145, 145, 127))
            .build()
    );

    private final Setting<Color> storageColor = sgColors.add(new ColorSetting.Builder()
            .name("storage-color")
            .description("The storage color.")
            .defaultValue(new Color(255, 160, 0, 127))
            .build()
    );

    private int count;

    public Tracers() {
        super(Category.Render, "tracers", "Displays tracer lines to specified entities.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if ((!ModuleManager.INSTANCE.isActive(Freecam.class) && entity == mc.player) || !entities.get().contains(entity.getType())) continue;

            if (entity instanceof PlayerEntity) {
                Color color = playersColor.get();
                Friend friend = FriendManager.INSTANCE.get(((PlayerEntity) entity).getGameProfile().getName());
                if (friend != null) color = friend.color;

                if (friend == null || friend.showInTracers) RenderUtils.drawTracerToEntity(event, entity, color, target.get(), stem.get()); count++;
            } else {
                switch (entity.getType().getSpawnGroup()) {
                    case CREATURE: RenderUtils.drawTracerToEntity(event, entity, animalsColor.get(), target.get(), stem.get()); count++; break;
                    case WATER_CREATURE: RenderUtils.drawTracerToEntity(event, entity, waterAnimalsColor.get(), target.get(), stem.get()); count++; break;
                    case MONSTER: RenderUtils.drawTracerToEntity(event, entity, monstersColor.get(), target.get(), stem.get()); count++; break;
                    case AMBIENT: RenderUtils.drawTracerToEntity(event, entity, ambientColor.get(), target.get(), stem.get()); count++; break;
                    case MISC: RenderUtils.drawTracerToEntity(event, entity, miscColor.get(), target.get(), stem.get()); count++; break;
                }
            }
        }

        if (storage.get()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity.isRemoved()) continue;

                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                    RenderUtils.drawTracerToBlockEntity(blockEntity, storageColor.get(), event);
                    count++;
                }
            }
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
