/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerDamageEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HitIndicators extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVisuals = settings.createGroup("视觉效果");

    // 添加事件监听器调试
    private boolean eventListenerRegistered = false;
    
    // 用于调试的属性

    // 目标实体类型设置
    private final Setting<Set<EntityType<?>>> targets = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("目标实体")
        .description("哪些实体被攻击时会显示指示器")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    // 忽略好友设置
    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("忽略好友")
        .description("不对好友攻击显示指示器")
        .defaultValue(false)
        .build()
    );

    // 显示时间设置
    private final Setting<Integer> duration = sgVisuals.add(new IntSetting.Builder()
        .name("显示时间")
        .description("指示器显示时间（毫秒）")
        .defaultValue(200)
        .min(100)
        .sliderMax(1000)
        .build()
    );

    // 命中颜色设置 - 高贵蓝色带透明
    private final Setting<SettingColor> hitColor = sgVisuals.add(new ColorSetting.Builder()
        .name("命中颜色")
        .description("命中时的颜色（高贵蓝色带透明）")
        .defaultValue(new SettingColor(0, 100, 255, 220)) // 高贵蓝色，更不透明
        .build()
    );

    // 渲染模式设置
    private final Setting<RenderMode> renderMode = sgVisuals.add(new EnumSetting.Builder<RenderMode>()
        .name("渲染模式")
        .description("实体渲染方式")
        .defaultValue(RenderMode.Box)
        .build()
    );

    // 渲染形状模式
    private final Setting<ShapeMode> shapeMode = sgVisuals.add(new EnumSetting.Builder<ShapeMode>()
        .name("形状模式")
        .description("渲染形状的模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    // 实体染色状态管理
    private static class EntityColored {
        public Entity entity;
        public Color color;
        public long startTime;
        public boolean hit;
        
        public EntityColored(Entity entity, Color color, long startTime, boolean hit) {
            this.entity = entity;
            this.color = color;
            this.startTime = startTime;
            this.hit = hit;
        }
    }
    
    private final List<EntityColored> coloredEntities = new ArrayList<>();

    public HitIndicators() {
        super(Categories.Combat, "hit-indicators", "玩家受伤击退时显示颜色指示器");
        // 关键设置：模块初始化后自动订阅事件
        this.autoSubscribe = true;
    }

    @Override
    public void onActivate() {
        System.out.println("[DEBUG] HitIndicators模块已激活");
        eventListenerRegistered = true;
        coloredEntities.clear();
    }

    @Override
    public void onDeactivate() {
        System.out.println("[DEBUG] HitIndicators模块已停用");
        eventListenerRegistered = false;
        coloredEntities.clear();
    }

    @EventHandler
    private void onPlayerDamage(PlayerDamageEvent event) {
        // 简单检查：只要是玩家受到伤害就显示指示器
        if (event.knockback > 0.01f) {
            addEntityColoring(mc.player, hitColor.get(), true);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        // 清理过期染色效果并渲染
        coloredEntities.removeIf(colored -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - colored.startTime >= duration.get() || !colored.entity.isAlive()) {
                return true; // 标记为移除
            }
            
            // 计算进度（0.0 到 1.0）
            float progress = (float) (currentTime - colored.startTime) / duration.get();
            
            // 渲染实体染色效果
            renderEntityColoring(colored, progress, event);
            
            return false;
        });
    }

    private void addEntityColoring(Entity entity, SettingColor settingColor, boolean hit) {
        // 创建颜色对象
        Color color = new Color(settingColor.r, settingColor.g, settingColor.b, settingColor.a);
        
        // 添加到染色列表
        coloredEntities.add(new EntityColored(entity, color, System.currentTimeMillis(), hit));
    }

    private void renderEntityColoring(EntityColored colored, float progress, Render3DEvent event) {
        // 计算透明度（从初始透明度渐变到透明）
        int alphaValue = (int) (colored.color.a * (1.0 - progress));
        Color renderColor = new Color(colored.color.r, colored.color.g, colored.color.b, alphaValue);

        // 渲染实体边界框（避免名称标签发光）
        renderEntityBox(colored.entity, renderColor, event);
    }

    private void renderEntityBox(Entity entity, Color color, Render3DEvent event) {
        // 使用与ESP模块相同的边界框计算方法（避免名称标签发光）
        double x = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
        double y = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
        double z = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();

        Box box = entity.getBoundingBox();
        event.renderer.box(
            x + box.minX, y + box.minY, z + box.minZ,
            x + box.maxX, y + box.maxY, z + box.maxZ,
            color, color,
            shapeMode.get(),
            0
        );
    }

    // 渲染模式枚举
    public enum RenderMode {
        Wireframe("线框"),
        Box("边界框");

        public final String name;

        RenderMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}