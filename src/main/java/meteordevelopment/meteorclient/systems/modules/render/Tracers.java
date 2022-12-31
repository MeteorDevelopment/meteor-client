/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
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
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    public final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Doesn't draw tracers to friends.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> showInvis = sgGeneral.add(new BoolSetting.Builder()
        .name("show-invisible")
        .description("Shows invisibile entities.")
        .defaultValue(true)
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

    private final Setting<Integer> maxDist = sgAppearance.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance for tracers to show.")
        .defaultValue(256)
        .min(0)
        .sliderMax(256)
        .build()
    );

    // Colors

    public final Setting<Boolean> distance = sgColors.add(new BoolSetting.Builder()
        .name("distance-colors")
        .description("Changes the color of tracers depending on distance.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> friendOverride = sgColors.add(new BoolSetting.Builder()
        .name("show-friend-colors")
        .description("Whether or not to override the distance color of friends with the friend color.")
        .defaultValue(true)
        .visible(() -> distance.get() && !ignoreFriends.get())
        .build()
    );

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
        .name("players-colors")
        .description("The player's color.")
        .defaultValue(new SettingColor(205, 205, 205, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
        .name("animals-color")
        .description("The animal's color.")
        .defaultValue(new SettingColor(145, 255, 145, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
        .name("water-animals-color")
        .description("The water animal's color.")
        .defaultValue(new SettingColor(145, 145, 255, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
        .name("monsters-color")
        .description("The monster's color.")
        .defaultValue(new SettingColor(255, 145, 145, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
        .name("ambient-color")
        .description("The ambient color.")
        .defaultValue(new SettingColor(75, 75, 75, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
        .name("misc-color")
        .description("The misc color.")
        .defaultValue(new SettingColor(145, 145, 145, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private int count;

    public Tracers() {
        super(Categories.Render, "tracers", "Displays tracer lines to specified entities.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.options.hudHidden) return;
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!PlayerUtils.isWithin(entity, maxDist.get()) || (!Modules.get().isActive(Freecam.class) && entity == mc.player) || !entities.get().getBoolean(entity.getType()) || (ignoreFriends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) || (!showInvis.get() && entity.isInvisible()) | !EntityUtils.isInRenderDistance(entity)) continue;

            Color color;

            if (distance.get()) {
                if (friendOverride.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
                    color = Config.get().friendColor.get();
                }
                else color = EntityUtils.getColorFromDistance(entity);
            }
            else if (entity instanceof PlayerEntity) {
                color = PlayerUtils.getPlayerColor(((PlayerEntity) entity), playersColor.get());
            }
            else {
                color = switch (entity.getType().getSpawnGroup()) {
                    case CREATURE -> animalsColor.get();
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> waterAnimalsColor.get();
                    case MONSTER -> monstersColor.get();
                    case AMBIENT -> ambientColor.get();
                    default -> miscColor.get();
                };
            }

            double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
            double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;

            double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
            if (target.get() == Target.Head) y += height;
            else if (target.get() == Target.Body) y += height / 2;

            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, color);
            if (stem.get()) event.renderer.line(x, entity.getY(), z, x, entity.getY() + height, z, color);

            count++;
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
