/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;

public class Tracers extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
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

    private final Setting<Boolean> offscreen = sgAppearance.add(new BoolSetting.Builder()
        .name("offscreen")
        .description("Uses a triangle-styled offscreen tracer style.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> distanceOffscreen = sgAppearance.add(new IntSetting.Builder()
        .name("distance-offscreen")
        .description("Offscreen's distance from center.")
        .defaultValue(200)
        .min(0)
        .sliderMax(500)
        .visible(() ->  offscreen.get())
        .build()
    );

    private final Setting<Integer> sizeOffscreen = sgAppearance.add(new IntSetting.Builder()
        .name("size-offscreen")
        .description("Offscreen's size.")
        .defaultValue(10)
        .min(2)
        .sliderMax(50)
        .visible(() ->  offscreen.get())
        .build()
    );

    private final Setting<Boolean> blinkOffscreen = sgAppearance.add(new BoolSetting.Builder()
        .name("blink-offscreen")
        .description("Make offscreen Blink.")
        .defaultValue(true)
        .visible(() ->  offscreen.get())
        .build()
    );

    private final Setting<Integer> blinkOffscreenSpeed = sgAppearance.add(new IntSetting.Builder()
        .name("blink-offscreen-speed")
        .description("Offscreen's blink speed.")
        .defaultValue(4)
        .min(1)
        .sliderMax(15)
        .visible(() ->  offscreen.get() && blinkOffscreen.get())
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
    private Instant initTimer = Instant.now();

    public Tracers() {
        super(Categories.Render, "tracers", "Displays tracer lines to specified entities.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.options.hudHidden || offscreen.get()) return;
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

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.options.hudHidden || !offscreen.get()) return;
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

            Color finalColor = new Color(color.r, color.g, color.b, color.a);
            if (blinkOffscreen.get())
                finalColor.a *= getAlpha();

            final Vec2f SCREEN_CENTER = new Vec2f(mc.getWindow().getFramebufferWidth() / 2.f, mc.getWindow().getFramebufferHeight() / 2.f);

            Vector3d projection = new Vector3d(entity.prevX, entity.prevY, entity.prevZ);
            NametagUtils.to2D(projection, 1, false, false);

            if (!(projection.x < 0.f || projection.x > mc.getWindow().getFramebufferWidth() || projection.y < 0.f || projection.y > mc.getWindow().getFramebufferHeight()))
                continue;

            projection = new Vector3d(entity.prevX, entity.prevY, entity.prevZ);
            NametagUtils.to2D(projection, 1, false, true);

            Vector2f angle = VectorAngles(new Vector3d(SCREEN_CENTER.x - projection.x, SCREEN_CENTER.y - projection.y, 0));
            angle.y += 180;

            MatrixStack matrices = RenderSystem.getModelViewStack();
            matrices.push();
            matrices.translate(0, 0, 0);
            matrices.scale(1.f, 1.f, 1);

            final float ANGLE_YAW_RAD = (float) Math.toRadians(angle.y);

            final Vector2f NEW_POINT = new Vector2f(SCREEN_CENTER.x + distanceOffscreen.get() * (float) Math.cos(ANGLE_YAW_RAD),
                SCREEN_CENTER.y + distanceOffscreen.get() * (float) Math.sin(ANGLE_YAW_RAD));

            Vector2f TRIANGLE_POINTS[] = {
                new Vector2f(NEW_POINT.x - sizeOffscreen.get(), NEW_POINT.y - sizeOffscreen.get()),
                new Vector2f(NEW_POINT.x + sizeOffscreen.get() * 0.73205f, NEW_POINT.y),
                new Vector2f(NEW_POINT.x - sizeOffscreen.get(), NEW_POINT.y + sizeOffscreen.get())
            };

            // rotate
            rotateTriangle(TRIANGLE_POINTS, angle.y);

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.triangle(TRIANGLE_POINTS[0].x, TRIANGLE_POINTS[0].y, TRIANGLE_POINTS[1].x, TRIANGLE_POINTS[1].y, TRIANGLE_POINTS[2].x,
                TRIANGLE_POINTS[2].y, finalColor);
            Renderer2D.COLOR.render(null);

            count++;

            RenderSystem.getModelViewStack().pop();
        }
    }

    private Vector2f addVec2f(Vector2f a, Vector2f b) {
        return new Vector2f(a.x + b.x, a.y + b.y);
    }

    private Vector2f subVec2f(Vector2f a, Vector2f b) {
        return new Vector2f(a.x - b.x, a.y - b.y);
    }

    private Vector2f divVec2f(Vector2f src, float fac) {
        return new Vector2f(src.x / fac, src.y / fac);
    }

    private void rotateTriangle(Vector2f[] points, float ang) {
        final Vector2f TRIANGLE_CENTER = divVec2f(addVec2f(addVec2f(points[0], points[1]), points[2]), 3);
        final float THETA = (float)Math.toRadians(ang);
        final float COS = (float)Math.cos(THETA);
        final float SIN = (float)Math.sin(THETA);
        for (int i = 0; i < 3; i++) {
            Vector2f point = new Vector2f(points[i].x, points[i].y);
            point = subVec2f(point, TRIANGLE_CENTER);

            Vector2f newPoint = new Vector2f(point.x * COS - point.y * SIN, point.x * SIN + point.y * COS);

            points[i] = addVec2f(newPoint, TRIANGLE_CENTER);
        }
    }

    private Vector2f VectorAngles(final Vector3d forward) {
        float tmp, yaw, pitch;

        if(forward.x == 0 && forward.y == 0) {
            yaw = 0;
            if(forward.z > 0)
                pitch = 270;
            else
                pitch = 90;
        } else {
            yaw = (float)(Math.atan2(forward.y, forward.x) * 180 / 3.1415f);
            if(yaw < 0)
                yaw += 360;

            tmp = (float)Math.sqrt(forward.x * forward.x + forward.y * forward.y);
            pitch = (float)(Math.atan2(-forward.z, tmp) * 180 / 3.1415f);
            if(pitch < 0)
                pitch += 360;
        }

        return new Vector2f(pitch, yaw);
    }

    private float getAlpha() {
        final float speed = blinkOffscreenSpeed.get() / 4.f;
        final double duration = Math.abs(Duration.between(Instant.now(), initTimer).toMillis()) * speed;
        final float seconds = (float)Math.abs((duration % 1000) - 500) / 500.f;

        return seconds;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
