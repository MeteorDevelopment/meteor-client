/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.entity.Target;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.entity.*;
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
            .defaultValue(Utils.asObject2BooleanOpenHashMap(EntityType.PLAYER))
            .build()
    );

    private final Setting<Boolean> storage = sgGeneral.add(new BoolSetting.Builder()
            .name("storage")
            .description("Displays storage blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> echests = sgGeneral.add(new BoolSetting.Builder()
            .name("echests")
            .description("Displays Enderchests blocks.")
            .defaultValue(false)
            .build()
    );

    // Appearance

    private final Setting<Integer> maxDist = sgAppearance.add(new IntSetting.Builder()
            .name("max-distance")
            .description("Maximum distance for tracers to show.")
            .defaultValue(256)
            .min(0)
            .sliderMax(256)
            .build()
    );

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

    public final Setting<Boolean> showInvis = sgGeneral.add(new BoolSetting.Builder()
            .name("show-invisible")
            .description("Shows invisibile entities.")
            .defaultValue(true)
            .build()
    );

    // Colors

    public final Setting<Boolean> distance = sgColors.add(new BoolSetting.Builder()
            .name("distance-colors")
            .description("Changes the color of tracers depending on distance.")
            .defaultValue(false)
            .build()
    );

    public final Setting<Boolean> useNameColor = sgColors.add(new BoolSetting.Builder()
            .name("use-name-color")
            .description("Uses players displayname color for the tracer color (good for minigames).")
            .defaultValue(false)
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

    private final Setting<SettingColor> echestColor = sgColors.add(new ColorSetting.Builder()
            .name("storage-color")
            .description("The storage color.")
            .defaultValue(new SettingColor(120, 0, 255, 255))
            .build()
    );

    private int count;

    public Tracers() {
        super(Categories.Render, "tracers", "Displays tracer lines to specified entities.");
    }


    @EventHandler
    private void onRender(RenderEvent event) {
        count = 0;
        for (Entity entity : mc.world.getEntities()) {
            if(mc.player.distanceTo(entity) > maxDist.get()) continue;
            if(entity instanceof PlayerEntity) if(Friends.get().contains(Friends.get().get((PlayerEntity) entity))) if(!Friends.get().show((PlayerEntity) entity)) continue;

            if ((!Modules.get().isActive(Freecam.class) && entity == mc.player) || !entities.get().getBoolean(entity.getType()) || (!showInvis.get() && entity.isInvisible())) continue;
            if (FakePlayerUtils.isFakePlayerOutOfRenderDistance(entity)) continue;

            Color color;
            if(!distance.get() || !(entity instanceof PlayerEntity) || Friends.get().contains(Friends.get().get((PlayerEntity) entity))) color = EntityUtils.getEntityColor(entity, playersColor.get(), animalsColor.get(), waterAnimalsColor.get(), monstersColor.get(), ambientColor.get(), miscColor.get(), useNameColor.get());
            else color = getColorFromDistance((PlayerEntity) entity);

            RenderUtils.drawTracerToEntity(event, entity, color, target.get(), stem.get());
            count++;
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
        if(echests.get()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity.isRemoved()) continue;
                if (blockEntity instanceof EnderChestBlockEntity) {
                    RenderUtils.drawTracerToBlockEntity(blockEntity, echestColor.get(), event);
                    count++;
                }
            }
        }
    }

    private SettingColor getColorFromDistance(PlayerEntity player) {
        //Credit to Icy from Stackoverflow
        assert mc.player != null;
        double distance = mc.player.distanceTo(player);
        double percent = distance / 60;
        {
            if (percent < 0 || percent > 1) { return new SettingColor(0, 255, 0, 255); }

            int r, g;
            if (percent < 0.5)
            {
                r = 255;
                g = (int) (255 * percent / 0.5);  //closer to 0.5, closer to yellow (255,255,0)
            }
            else
            {
                g = 255;
                r = 255 - (int) (255 * (percent - 0.5) / 0.5); //closer to 1.0, closer to green (0,255,0)
            }
            return new SettingColor(r, g, 0, 255);
        }
    }


    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
