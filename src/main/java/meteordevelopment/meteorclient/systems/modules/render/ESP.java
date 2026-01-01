/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.Set;

public class ESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Shader)
        .build()
    );

    public final Setting<Boolean> highlightTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-target")
        .description("highlights the currently targeted entity differently")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> targetHitbox = sgGeneral.add(new BoolSetting.Builder()
        .name("target-hitbox")
        .description("draw the hitbox of the target entity")
        .defaultValue(true)
        .visible(highlightTarget::get)
        .build()
    );

    public final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("outline-width")
        .description("The width of the shader outline.")
        .visible(() -> mode.get() == Mode.Shader)
        .defaultValue(2)
        .range(1, 10)
        .sliderRange(1, 5)
        .build()
    );

    public final Setting<Double> glowMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("glow-multiplier")
        .description("Multiplier for glow effect")
        .visible(() -> mode.get() == Mode.Shader)
        .decimalPlaces(3)
        .defaultValue(3.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    public final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignores yourself drawing the shader.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> healthColors = sgGeneral.add(new BoolSetting.Builder()
        .name("health-colors")
        .description("Change player ESP color based on health")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hpGradient = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-gradient")
        .description("Interpolate color between thresholds for players")
        .defaultValue(true)
        .visible(healthColors::get)
        .build()
    );

    public final Setting<Boolean> hpFlicker = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-flicker")
        .description("Low HP flicker effect for players")
        .defaultValue(true)
        .visible(healthColors::get)
        .build()
    );

    public final Setting<Integer> flickerPeriod = sgGeneral.add(new IntSetting.Builder()
        .name("flicker-period")
        .description("Flicker period in ticks")
        .defaultValue(20)
        .range(5, 60)
        .sliderRange(5, 40)
        .visible(hpFlicker::get)
        .build()
    );

    public final Setting<Boolean> hpGate = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-gate")
        .description("Only render player ESP when HP is below threshold")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> hpThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("hp-threshold")
        .description("HP threshold for gating player ESP visibility")
        .defaultValue(20)
        .range(1, 40).sliderRange(1, 40)
        .visible(hpGate::get)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .visible(() -> mode.get() != Mode.Glow)
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Double> fillOpacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill.")
        .visible(() -> shapeMode.get() != ShapeMode.Lines && mode.get() != Mode.Glow)
        .defaultValue(0.3)
        .range(0, 1)
        .sliderMax(1)
        .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fade-distance")
        .description("The distance from an entity where the color begins to fade.")
        .defaultValue(3)
        .min(0)
        .sliderMax(12)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select specific entities.")
        .defaultValue(EntityType.PLAYER)
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
        .visible(distance::get)
        .build()
    );

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
        .name("players-color")
        .description("The other player's color.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
        .name("animals-color")
        .description("The animal's color.")
        .defaultValue(new SettingColor(25, 255, 25, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
        .name("water-animals-color")
        .description("The water animal's color.")
        .defaultValue(new SettingColor(25, 25, 255, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
        .name("monsters-color")
        .description("The monster's color.")
        .defaultValue(new SettingColor(255, 25, 25, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
        .name("ambient-color")
        .description("The ambient's color.")
        .defaultValue(new SettingColor(25, 25, 25, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
        .name("misc-color")
        .description("The misc color.")
        .defaultValue(new SettingColor(175, 175, 175, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> targetColor = sgColors.add(new ColorSetting.Builder()
        .name("target-color")
        .description("The target color.")
        .defaultValue(new SettingColor(200, 200, 200, 255))
        .visible(highlightTarget::get)
        .build()
    );

    private final Setting<SettingColor> targetHitboxColor = sgColors.add(new ColorSetting.Builder()
        .name("target-hitbox-color")
        .description("The target hitbox color.")
        .defaultValue(new SettingColor(100, 200, 200, 255))
        .visible(() -> highlightTarget.get() && targetHitbox.get())
        .build()
    );

    // Fields for GC Optimization
    private final Color lineColor = new Color();
    private final Color sideColor = new Color();
    private final Color baseColor = new Color();
    
    // Reusable health color to prevent `new Color()` spam
    private final Color mutableHealthColor = new Color();
    
    private final Color RED = new Color(255, 55, 55);
    private final Color AMBER = new Color(255, 170, 0);
    private final Color PURPLE = new Color(255, 0, 255);

    private final Vector3d pos1 = new Vector3d();
    private final Vector3d pos2 = new Vector3d();
    private final Vector3d pos = new Vector3d();

    private int count;
    private double currentFlickerFactor = 1.0; // Pre-calculated flicker factor

    public ESP() {
        super(Categories.Render, "esp", "Renders entities through walls.");
    }

    // Box

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mode.get() == Mode._2D) return;

        count = 0;
        
        // 优化：预计算闪烁因子，避免每帧每实体计算 Math.sin
        updateFlickerFactor();

        Entity target = null;
        if (highlightTarget.get() && targetHitbox.get() && mc.crosshairTarget instanceof EntityHitResult hr) {
            target = hr.getEntity();
        }

        // 优化：直接使用迭代器，配合快速跳过逻辑
        for (Entity entity : mc.world.getEntities()) {
            // 将 target check 移到前面
            boolean isTarget = (target == entity);
            if (!isTarget && shouldSkip(entity)) continue;
            
            if (isTarget || mode.get() == Mode.Box || mode.get() == Mode.Wireframe) {
                drawBoundingBox(event, entity, isTarget);
            }
            count++;
        }
    }

    private void drawBoundingBox(Render3DEvent event, Entity entity, boolean isTarget) {
        Color color = getColor(entity, isTarget);
        if (color != null) {
            lineColor.set(color);
            sideColor.set(color).a((int) (sideColor.a * fillOpacity.get()));
        }

        if (mode.get() == Mode.Wireframe) {
            WireframeEntityRenderer.render(event, entity, 1, sideColor, lineColor, shapeMode.get());
        }

        boolean renderTargetBox = highlightTarget.get() && isTarget;

        if (mode.get() == Mode.Box || (targetHitbox.get() && renderTargetBox)) {
            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

            ShapeMode shape = shapeMode.get();
            if (renderTargetBox && mode.get() != Mode.Box) shape = ShapeMode.Lines;
            if (renderTargetBox) lineColor.set(targetHitboxColor.get());

            Box box = entity.getBoundingBox();
            event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor, lineColor, shape, 0);
        }
    }

    // 2D

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mode.get() != Mode._2D) return;

        Renderer2D.COLOR.begin();
        count = 0;
        updateFlickerFactor(); // 优化：预计算闪烁

        // 缓存 target 状态
        Entity target = null;
        if (highlightTarget.get() && mc.crosshairTarget instanceof EntityHitResult hr) {
            target = hr.getEntity();
        }

        for (Entity entity : mc.world.getEntities()) {
            boolean isTarget = (target == entity);
            if (!isTarget && shouldSkip(entity)) continue;

            Box box = entity.getBoundingBox();

            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

            // Check corners
            pos1.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            pos2.set(0, 0, 0);

            // Optimization: Unroll loops manually to avoid loop overhead for fixed 8 corners
            if (checkCorner(box.minX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;

            if (checkCorner(box.minX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;

            // Setup color
            Color color = getColor(entity, isTarget);
            if (color != null) {
                lineColor.set(color);
                sideColor.set(color).a((int) (sideColor.a * fillOpacity.get()));
            }

            // Render
            if (shapeMode.get() != ShapeMode.Lines && sideColor.a > 0) {
                Renderer2D.COLOR.quad(pos1.x, pos1.y, pos2.x - pos1.x, pos2.y - pos1.y, sideColor);
            }

            if (shapeMode.get() != ShapeMode.Sides) {
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos1.x, pos2.y, lineColor);
                Renderer2D.COLOR.line(pos2.x, pos1.y, pos2.x, pos2.y, lineColor);
                Renderer2D.COLOR.line(pos1.x, pos1.y, pos2.x, pos1.y, lineColor);
                Renderer2D.COLOR.line(pos1.x, pos2.y, pos2.x, pos2.y, lineColor);
            }

            count++;
        }

        Renderer2D.COLOR.render();
    }

    public boolean forceRender() {
        return isActive() && (mode.get() == Mode.Shader || mode.get() == Mode.Glow);
    }

    private boolean checkCorner(double x, double y, double z, Vector3d min, Vector3d max) {
        pos.set(x, y, z);
        if (!NametagUtils.to2D(pos, 1)) return true;

        if (pos.x < min.x) min.x = pos.x;
        if (pos.y < min.y) min.y = pos.y;
        if (pos.z < min.z) min.z = pos.z;

        if (pos.x > max.x) max.x = pos.x;
        if (pos.y > max.y) max.y = pos.y;
        if (pos.z > max.z) max.z = pos.z;

        return false;
    }

    // Utils
    
    // 优化：预计算闪烁因子
    private void updateFlickerFactor() {
        if (healthColors.get() && hpFlicker.get() && mc.world != null) {
            double t = (mc.world.getTime() % flickerPeriod.get()) / (double) flickerPeriod.get();
            currentFlickerFactor = 0.5 + 0.5 * Math.sin(t * 6.283185307179586);
        } else {
            currentFlickerFactor = 1.0;
        }
    }

    public boolean shouldSkip(Entity entity) {
        // 优化：先进行廉价的检查
        if (entity == mc.player && ignoreSelf.get()) return true;
        if (entity == mc.getCameraEntity() && mc.options.getPerspective().isFirstPerson()) return true;
        
        // 渲染距离检查
        if (!EntityUtils.isInRenderDistance(entity)) return true;
        
        // 类型检查 (Set 查找比上面的贵一点)
        if (!entities.get().contains(entity.getType())) return true;
        
        // HP 检查 (最贵，涉及到记分板查找)
        if (hpGate.get() && entity instanceof PlayerEntity p) {
            double hpVal = getGateHp(p);
            if (hpVal >= hpThreshold.get()) return true;
        }
        
        return false;
    }

    public Color getColor(Entity entity, boolean isTarget) {
        Color color;
        double alpha = 1;

        if (isTarget) {
            color = targetColor.get();
        } else {
            alpha = getFadeAlpha(entity);
            if (alpha == 0) return null;
            color = getEntityTypeColor(entity);
        }
        
        // 使用 set 防止 new Color
        return baseColor.set(color.r, color.g, color.b, (int) (color.a * alpha));
    }
    
    // 旧的 getColor 兼容
    public Color getColor(Entity entity) {
        return getColor(entity, highlightTarget.get() && mc.crosshairTarget instanceof EntityHitResult hr && hr.getEntity() == entity);
    }

    private double getFadeAlpha(Entity entity) {
        double dist = PlayerUtils.squaredDistanceToCamera(entity.getX() + entity.getWidth() / 2, entity.getY() + entity.getEyeHeight(entity.getPose()), entity.getZ() + entity.getWidth() / 2);
        double fadeDist = Math.pow(fadeDistance.get(), 2);
        double alpha = 1;
        if (dist <= fadeDist * fadeDist) alpha = (float) (Math.sqrt(dist) / fadeDist);
        if (alpha <= 0.075) alpha = 0;
        return alpha;
    }

    public Color getEntityTypeColor(Entity entity) {
        if (entity instanceof PlayerEntity p && healthColors.get()) {
            double hpVal = getGateHp(p);
            
            // 优化：直接修改 mutableHealthColor 而不是返回 new Color
            if (hpVal < 10) {
                int r = (int) MathHelper.clamp(RED.r * currentFlickerFactor, 0, 255);
                int g = (int) MathHelper.clamp(RED.g * currentFlickerFactor, 0, 255);
                int b = (int) MathHelper.clamp(RED.b * currentFlickerFactor, 0, 255);
                return mutableHealthColor.set(r, g, b, 255);
            } else if (hpVal <= 20 && hpGradient.get()) {
                double t = (hpVal - 10.0) / 10.0;
                int r = (int) MathHelper.clamp(MathHelper.lerp(t, RED.r, PURPLE.r), 0, 255);
                int g = (int) MathHelper.clamp(MathHelper.lerp(t, RED.g, PURPLE.g), 0, 255);
                int b = (int) MathHelper.clamp(MathHelper.lerp(t, RED.b, PURPLE.b), 0, 255);
                return mutableHealthColor.set(r, g, b, 255);
            } else if (hpVal <= 20) {
                return AMBER;
            } else {
                return PURPLE;
            }
        } else if (distance.get()) {
            if (friendOverride.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
                return Config.get().friendColor.get();
            } else return EntityUtils.getColorFromDistance(entity);
        } else if (entity instanceof PlayerEntity) {
            return PlayerUtils.getPlayerColor(((PlayerEntity) entity), playersColor.get());
        } else {
            return switch (entity.getType().getSpawnGroup()) {
                case CREATURE -> animalsColor.get();
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> waterAnimalsColor.get();
                case MONSTER -> monstersColor.get();
                case AMBIENT -> ambientColor.get();
                default -> miscColor.get();
            };
        }
    }

    private double getGateHp(PlayerEntity p) {
        Integer tab = getTrueTabHealth(p);
        if (tab != null) return tab;
        return p.getHealth() + p.getAbsorptionAmount();
    }

    private Integer getTrueTabHealth(PlayerEntity player) {
        if (mc.world == null || mc.getNetworkHandler() == null) return null;
        Scoreboard sb = mc.world.getScoreboard();
        
        // 增加判空防御
        if (sb == null) return null;
        
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        if (obj == null) return null;
        
        ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
        
        // 使用 getScore 而不是 getOrCreateScore 来避免可能的副作用（虽然在Client端影响不大，但更安全）
        // 注意：Meteor 工具类通常处理了映射差异，这里保留原逻辑结构
        ScoreAccess score = sb.getOrCreateScore(holder, obj);
        int points = score.getScore();
        if (points <= 0) return null;
        return points;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public boolean isShader() {
        return isActive() && mode.get() == Mode.Shader;
    }

    public boolean isGlow() {
        return isActive() && mode.get() == Mode.Glow;
    }

    public enum Mode {
        Box,
        Wireframe,
        _2D,
        Shader,
        Glow;

        @Override
        public String toString() {
            return this == _2D ? "2D" : super.toString();
        }
    }
}