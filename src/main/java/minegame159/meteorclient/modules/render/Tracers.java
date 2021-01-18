/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.rendering.ColorStyle;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.Target;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;

public class Tracers extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new Object2BooleanOpenHashMap<>(0))
            .build()
    );

    private final Setting<Boolean> storage = sgGeneral.add(new BoolSetting.Builder()
            .name("storage")
            .description("Displays storage blocks.")
            .defaultValue(false)
            .build()
    );

    // Appearance

    private final Setting<Target> target = sgAppearance.add(new EnumSetting.Builder<Target>()
            .name("target")
            .description("What part of the entity to target.")
            .defaultValue(Target.Body)
            .build()
    );

    private final Setting<Boolean> stem = sgAppearance.add(new BoolSetting.Builder()
            .name("stem")
            .description("Draw a line through the center of the tracer target.")
            .defaultValue(true)
            .build()
    );

    // Colors
    private final Setting<ColorStyle> colorStyle = sgColors.add(new EnumSetting.Builder<ColorStyle>()
            .name("color-style")
            .description("Choose between fixed-color highlight, or based off the user's nametag")
            .defaultValue(ColorStyle.Fixed)
            .build()
    );

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
            .name("players-colors")
            .description("The player's color.")
            .defaultValue(new SettingColor(205, 205, 205, 127))
            .build()
    );

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("The animal's color.")
            .defaultValue(new SettingColor(145, 255, 145, 127))
            .build()
    );

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
            .name("water-animals-color")
            .description("The water animal's color.")
            .defaultValue(new SettingColor(145, 145, 255, 127))
            .build()
    );

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("The monster's color.")
            .defaultValue(new SettingColor(255, 145, 145, 127))
            .build()
    );

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
            .name("ambient-color")
            .description("The ambient color.")
            .defaultValue(new SettingColor(75, 75, 75, 127))
            .build()
    );

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("The misc color.")
            .defaultValue(new SettingColor(145, 145, 145, 127))
            .build()
    );

    private final Setting<SettingColor> storageColor = sgColors.add(new ColorSetting.Builder()
            .name("storage-color")
            .description("The storage color.")
            .defaultValue(new SettingColor(255, 160, 0, 127))
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
            if ((!ModuleManager.INSTANCE.isActive(Freecam.class) && entity == mc.player) || !entities.get().getBoolean(entity.getType())) continue;

            if (entity instanceof PlayerEntity) {
                Color color = colorStyle.get() == ColorStyle.Fixed ? playersColor.get() : TextUtils.getMostPopularColor(entity.getDisplayName());

                Friend friend = FriendManager.INSTANCE.get((PlayerEntity) entity);
                if (friend != null) color = FriendManager.INSTANCE.getColor((PlayerEntity) entity, color, false);

                if (friend == null || FriendManager.INSTANCE.show((PlayerEntity) entity)) RenderUtils.drawTracerToEntity(event, entity, color, target.get(), stem.get()); count++;
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
