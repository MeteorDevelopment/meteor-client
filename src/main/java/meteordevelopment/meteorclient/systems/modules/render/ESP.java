/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 * Optimized for performance & fixed logical features (Flicker, Gradient, Raycast Target).
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
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
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.Set;
import java.util.UUID;

public class ESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors  = settings.createGroup("Colors");

    // ==================== General ====================

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Shader)
        .build()
    );

    public final Setting<Boolean> highlightTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("highlight-target")
        .description("Highlights the currently targeted entity differently.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> targetHitbox = sgGeneral.add(new BoolSetting.Builder()
        .name("target-hitbox")
        .description("Draw the hitbox of the target entity.")
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
        .description("Multiplier for glow effect.")
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
        .description("Change player ESP color based on health.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hpGradient = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-gradient")
        .description("Interpolate color between thresholds for players.")
        .defaultValue(true)
        .visible(healthColors::get)
        .build()
    );

    public final Setting<Boolean> hpFlicker = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-flicker")
        .description("Low HP flicker effect for players.")
        .defaultValue(true)
        .visible(healthColors::get)
        .build()
    );

    public final Setting<Integer> flickerPeriod = sgGeneral.add(new IntSetting.Builder()
        .name("flicker-period")
        .description("Flicker period in ticks.")
        .defaultValue(20)
        .range(5, 60)
        .sliderRange(5, 40)
        .visible(hpFlicker::get)
        .build()
    );

    public final Setting<Boolean> hpGate = sgGeneral.add(new BoolSetting.Builder()
        .name("hp-gate")
        .description("Only render player ESP when HP is below threshold.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> hpThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("hp-threshold")
        .description("血量等于或低于此值时才显示ESP (满血通常为20)。")
        .defaultValue(19) // 默认改成19，如果是20的话满血也会被渲染出来
        .range(1, 40)
        .sliderRange(1, 40)
        .visible(hpGate::get)
        .build()
    );

    // ==================== 血量来源模式 ====================

    public enum HealthSource {
        Tab,    // 从计分板 Tab 列表读取（服务器推送的真实血量，适用于支持的服务器）
        Client  // 从客户端实体数据读取（本地可见血量，通用兼容）
    }

    public final Setting<HealthSource> healthSource = sgGeneral.add(new EnumSetting.Builder<HealthSource>()
        .name("health-source")
        .description("Tab可以绕过某些服务器强制修改客户端满血的反作弊。")
        .defaultValue(HealthSource.Tab) // 建议默认用Tab应对实战服务器
        .visible(healthColors::get)
        .build()
    );

    // ==================== Shape / Fade ====================

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
        .defaultValue(0.1)
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

    // ==================== Colors ====================

    public final Setting<Boolean> distance = sgColors.add(new BoolSetting.Builder()
        .name("distance-colors")
        .description("Changes the color of tracers depending on distance.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> friendOverride = sgColors.add(new BoolSetting.Builder()
        .name("show-friend-colors")
        .description("Override the distance color of friends with the friend color.")
        .defaultValue(true)
        .visible(distance::get)
        .build()
    );

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
        .name("players-color")
        .description("The other player's color.")
        .defaultValue(new SettingColor(220, 220, 220))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
        .name("animals-color")
        .defaultValue(new SettingColor(150, 255, 150, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
        .name("water-animals-color")
        .defaultValue(new SettingColor(150, 150, 255, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
        .name("monsters-color")
        .defaultValue(new SettingColor(255, 150, 150, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
        .name("ambient-color")
        .defaultValue(new SettingColor(150, 150, 150, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
        .name("misc-color")
        .defaultValue(new SettingColor(200, 200, 200, 255))
        .visible(() -> !distance.get())
        .build()
    );

    private final Setting<SettingColor> targetColor = sgColors.add(new ColorSetting.Builder()
        .name("target-color")
        .defaultValue(new SettingColor(230, 230, 230, 255))
        .visible(highlightTarget::get)
        .build()
    );

    private final Setting<SettingColor> targetHitboxColor = sgColors.add(new ColorSetting.Builder()
        .name("target-hitbox-color")
        .defaultValue(new SettingColor(150, 220, 220, 255))
        .visible(() -> highlightTarget.get() && targetHitbox.get())
        .build()
    );

    // ==================== State ====================

    private final Color lineColor          = new Color();
    private final Color sideColor          = new Color();
    private final Color baseColor          = new Color();
    private final Color mutableHealthColor = new Color();

    private final Color LOW_HP    = new Color(255, 120, 120);
    private final Color MEDIUM_HP = new Color(255, 230, 150);
    private final Color HIGH_HP   = new Color(150, 255, 190);

    private final Vector3d pos1 = new Vector3d();
    private final Vector3d pos2 = new Vector3d();
    private final Vector3d pos  = new Vector3d();

    private int count;
    private Entity cachedTarget = null;
    private final Object2IntMap<UUID> tabHealthCache = new Object2IntOpenHashMap<>();

    public ESP() {
        super(Categories.Render, "esp", "Renders entities through walls. Optimized Edition.");
    }

    @Override
    public void onDeactivate() {
        tabHealthCache.clear();
    }

    // ==================== Tick ====================

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tabHealthCache.clear();
        if (healthSource.get() != HealthSource.Tab) return;
        if (mc.world == null) return;

        Scoreboard sb  = mc.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        if (obj == null) obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (obj != null) {
                try {
                    ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
                    ReadableScoreboardScore score = sb.getScore(holder, obj);
                    if (score != null && score.getScore() > 0) {
                        tabHealthCache.put(player.getUuid(), score.getScore());
                        continue;
                    }
                } catch (Exception ignored) {}
            }
            tabHealthCache.put(player.getUuid(), (int)(player.getHealth() + player.getAbsorptionAmount()));
        }
    }

    // ==================== 血量获取 ====================

    private double getHp(PlayerEntity p) {
        if (healthSource.get() == HealthSource.Tab) {
            UUID uuid = p.getUuid();
            if (tabHealthCache.containsKey(uuid)) {
                return tabHealthCache.getInt(uuid);
            }
        }
        return p.getHealth() + p.getAbsorptionAmount();
    }

    // ==================== Render 3D ====================

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mode.get() == Mode._2D) return;
        count = 0;
        cachedTarget = highlightTarget.get() ? getTargetEntity(100.0, event.tickDelta) : null;

        for (Entity entity : mc.world.getEntities()) {
            boolean isTarget = (cachedTarget == entity);
            if (!isTarget && shouldSkip(entity)) continue;

            if (isTarget || mode.get() == Mode.Box || mode.get() == Mode.Wireframe) {
                drawBoundingBox(event, entity, isTarget);
            }
            count++;
        }
    }

    private void drawBoundingBox(Render3DEvent event, Entity entity, boolean isTarget) {
        Color color = getColor(entity, isTarget);
        if (color == null) return;

        lineColor.set(color);
        sideColor.set(color).a((int) (sideColor.a * fillOpacity.get()));

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
            event.renderer.box(x + box.minX, y + box.minY, z + box.minZ,
                               x + box.maxX, y + box.maxY, z + box.maxZ,
                               sideColor, lineColor, shape, 0);
        }
    }

    // ==================== Render 2D ====================

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mode.get() != Mode._2D) return;

        Renderer2D.COLOR.begin();
        count = 0;
        cachedTarget = highlightTarget.get() ? getTargetEntity(100.0, event.tickDelta) : null;

        for (Entity entity : mc.world.getEntities()) {
            boolean isTarget = (cachedTarget == entity);
            if (!isTarget && shouldSkip(entity)) continue;

            Box box = entity.getBoundingBox();
            double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

            pos1.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            pos2.set(0, 0, 0);

            if (checkCorner(box.minX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.minY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.minZ + z, pos1, pos2)) continue;
            if (checkCorner(box.minX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;
            if (checkCorner(box.maxX + x, box.maxY + y, box.maxZ + z, pos1, pos2)) continue;

            Color color = getColor(entity, isTarget);
            if (color == null) continue;

            lineColor.set(color);
            sideColor.set(color).a((int) (sideColor.a * fillOpacity.get()));

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

    // ==================== Logic ====================

    public boolean shouldSkip(Entity entity) {
        if (entity == mc.player && ignoreSelf.get()) return true;
        if (entity == mc.getCameraEntity() && mc.options.getPerspective().isFirstPerson()) return true;
        if (!EntityUtils.isInRenderDistance(entity)) return true;
        if (!entities.get().contains(entity.getType())) return true;

        if (hpGate.get() && entity instanceof PlayerEntity p) {
            double hpVal = getHp(p);
            if (hpVal > hpThreshold.get()) return true;
        }

        return false;
    }

    public Color getColor(Entity entity, boolean isTarget) {
        if (isTarget) {
            return baseColor.set(targetColor.get());
        }

        double alpha = getFadeAlpha(entity);
        if (alpha == 0) return null;

        Color color = getEntityTypeColor(entity);
        return baseColor.set(color.r, color.g, color.b, (int) (color.a * alpha));
    }

    // 【核心修复区域】=============
    public Color getColor(Entity entity) {
        // Meteor 的 Shader 和 Glow 模式依赖这个外部 API 获取颜色
        boolean isTarget = highlightTarget.get() && entity == cachedTarget;
        
        // 之前少写了这一行，导致在 Shader 模式下完全无视了任何跳过逻辑（不过滤满血玩家、不过滤猪牛羊等）
        if (!isTarget && shouldSkip(entity)) return null;

        return getColor(entity, isTarget);
    }
    // ============================

    private double getFadeAlpha(Entity entity) {
        double distSq = PlayerUtils.squaredDistanceToCamera(
            entity.getX() + entity.getWidth() / 2,
            entity.getY() + entity.getEyeHeight(entity.getPose()),
            entity.getZ() + entity.getWidth() / 2
        );

        double fd   = fadeDistance.get();
        double fdSq = fd * fd;

        if (distSq <= fdSq && fd > 0) {
            double alpha = Math.sqrt(distSq) / fd;
            return alpha <= 0.075 ? 0 : alpha;
        }
        return 1.0;
    }

    public Color getEntityTypeColor(Entity entity) {
        if (entity instanceof PlayerEntity p && healthColors.get()) {
            double hpVal = getHp(p);

            // 致命血量：红白闪烁
            if (hpVal <= 5) {
                if (hpFlicker.get()) {
                    long cycle  = flickerPeriod.get() * 50L;
                    double time = System.currentTimeMillis() % cycle;
                    double sine = (Math.sin((time / cycle) * Math.PI * 2) + 1.0) / 2.0;
                    int r = (int) MathHelper.lerp(sine, LOW_HP.r, 255);
                    int g = (int) MathHelper.lerp(sine, LOW_HP.g, 255);
                    int b = (int) MathHelper.lerp(sine, LOW_HP.b, 255);
                    return mutableHealthColor.set(r, g, b, 255);
                }
                return mutableHealthColor.set(LOW_HP);
            }
            // 正常区间渐变：红 → 橙 → 绿
            else if (hpVal <= 20) {
                if (hpGradient.get()) {
                    if (hpVal <= 10) {
                        double t = hpVal / 10.0;
                        return mutableHealthColor.set(
                            (int) MathHelper.lerp(t, LOW_HP.r, MEDIUM_HP.r),
                            (int) MathHelper.lerp(t, LOW_HP.g, MEDIUM_HP.g),
                            (int) MathHelper.lerp(t, LOW_HP.b, MEDIUM_HP.b),
                            255
                        );
                    } else {
                        double t = (hpVal - 10.0) / 10.0;
                        return mutableHealthColor.set(
                            (int) MathHelper.lerp(t, MEDIUM_HP.r, HIGH_HP.r),
                            (int) MathHelper.lerp(t, MEDIUM_HP.g, HIGH_HP.g),
                            (int) MathHelper.lerp(t, MEDIUM_HP.b, HIGH_HP.b),
                            255
                        );
                    }
                }
                return hpVal <= 10
                    ? mutableHealthColor.set(LOW_HP)
                    : mutableHealthColor.set(MEDIUM_HP);
            }
            // 满血
            else {
                return mutableHealthColor.set(HIGH_HP);
            }
        }
        else if (distance.get()) {
            if (friendOverride.get() && entity instanceof PlayerEntity pe && Friends.get().isFriend(pe)) {
                return Config.get().friendColor.get();
            }
            return EntityUtils.getColorFromDistance(entity);
        }
        else if (entity instanceof PlayerEntity pe) {
            return PlayerUtils.getPlayerColor(pe, playersColor.get());
        }
        else {
            return switch (entity.getType().getSpawnGroup()) {
                case CREATURE                                                          -> animalsColor.get();
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> waterAnimalsColor.get();
                case MONSTER                                                           -> monstersColor.get();
                case AMBIENT                                                           -> ambientColor.get();
                default                                                                -> miscColor.get();
            };
        }
    }

    // ==================== Helpers ====================

    private Entity getTargetEntity(double range, float tickDelta) {
        if (mc.getCameraEntity() == null || mc.world == null) return null;
        Entity camera    = mc.getCameraEntity();
        Vec3d cameraPos  = camera.getCameraPosVec(tickDelta);
        Vec3d rotVec     = camera.getRotationVec(tickDelta);
        Vec3d endPos     = cameraPos.add(rotVec.x * range, rotVec.y * range, rotVec.z * range);

        Entity closest     = null;
        double closestDist = range * range;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entities.get().contains(entity.getType())) continue;
            Box box = entity.getBoundingBox().expand(0.3);
            if (box.raycast(cameraPos, endPos).isPresent()) {
                double dist = camera.squaredDistanceTo(entity);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest     = entity;
                }
            }
        }
        return closest;
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

    public boolean forceRender() {
        return isActive() && (mode.get() == Mode.Shader || mode.get() == Mode.Glow);
    }

    public boolean isShader() {
        return isActive() && mode.get() == Mode.Shader;
    }

    public boolean isGlow() {
        return isActive() && mode.get() == Mode.Glow;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
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