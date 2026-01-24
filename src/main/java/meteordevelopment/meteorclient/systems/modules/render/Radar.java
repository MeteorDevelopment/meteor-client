/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Radar extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General
    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
        .name("size")
        .description("雷达的大小，以像素为单位")
        .defaultValue(100)
        .min(50)
        .max(300)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("雷达的检测范围，以米为单位")
        .defaultValue(50)
        .min(10)
        .max(200)
        .build()
    );

    private final Setting<Integer> x = sgGeneral.add(new IntSetting.Builder()
        .name("x")
        .description("雷达左上角的X坐标")
        .defaultValue(50)
        .min(0)
        .build()
    );

    private final Setting<Integer> y = sgGeneral.add(new IntSetting.Builder()
        .name("y")
        .description("雷达左上角的Y坐标")
        .defaultValue(50)
        .min(0)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("忽略自己")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("忽略好友/队友 (如果不勾选，队友将显示在雷达上)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-bots")
        .description("忽略机器人")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> showFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("show-friends")
        .description("显示队友 (二次确认)")
        .defaultValue(true)
        .build()
    );

    // Colors
    private final Setting<Boolean> healthColors = sgColors.add(new BoolSetting.Builder()
        .name("health-colors")
        .description("根据血量改变玩家指示器颜色 (仅对敌人生效)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hpFlicker = sgColors.add(new BoolSetting.Builder()
        .name("hp-flicker")
        .description("低血量闪烁效果")
        .defaultValue(true)
        .visible(healthColors::get)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background-color")
        .description("雷达背景的颜色")
        .defaultValue(new SettingColor(0, 0, 0, 100))
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("雷达边框的颜色")
        .defaultValue(new SettingColor(255, 255, 255, 200))
        .build()
    );

    private final Setting<SettingColor> playerColor = sgColors.add(new ColorSetting.Builder()
        .name("player-color")
        .description("玩家指示器的颜色 (敌人)")
        .defaultValue(new SettingColor(255, 0, 0, 255)) // 红色
        .build()
    );

    private final Setting<SettingColor> friendColor = sgColors.add(new ColorSetting.Builder()
        .name("friend-color")
        .description("好友/队友指示器的颜色")
        .defaultValue(new SettingColor(0, 0, 255, 255)) // 蓝色
        .build()
    );

    private final Setting<SettingColor> ownColor = sgColors.add(new ColorSetting.Builder()
        .name("own-color")
        .description("自己指示器的颜色")
        .defaultValue(new SettingColor(0, 255, 255, 255)) // 青色
        .build()
    );

    // 血量颜色常量
    private final Color CRITICAL_HP = new Color(139, 0, 0);     
    private final Color LOW_HP = new Color(255, 40, 40);       
    private final Color MEDIUM_HP = new Color(255, 255, 255);  
    private final Color HIGH_HP = new Color(0, 255, 127);      

    // 血量缓存
    private final HashMap<Long, Double> cachedHealthMap = new HashMap<>();
    private long lastHealthCacheUpdate = 0;
    private static final long HEALTH_CACHE_INTERVAL = 500; 

    // 闪烁效果
    private long lastFlickerUpdate = 0;
    private int flickerTick = 0;

    public Radar() {
        super(Categories.Render, "radar", "显示一个雷达，指向附近的所有玩家");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        List<PlayerEntity> players = new ArrayList<>();

        // 收集附近的玩家
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (ignoreSelf.get() && player == mc.player) continue;
            
            // 距离初步筛选
            if (PlayerUtils.distanceToCamera(player) > range.get()) continue;

            // 队友判定
            boolean isTeam = isTeammate(player);

            // 机器人检测 (仅对非好友生效)
            if (!isTeam && ignoreBots.get() && EntityUtils.getGameMode(player) == null) continue;

            // 队友处理
            if (isTeam) {
                if (!ignoreFriends.get()) {
                    players.add(player);
                }
            } else {
                // 敌人处理
                players.add(player);
            }
        }

        // 计算雷达中心和半径
        int radarSize = size.get();
        int radarX = x.get(); 
        int radarY = y.get();
        int centerX = radarX + radarSize / 2;
        int centerY = radarY + radarSize / 2;
        int radius = radarSize / 2;

        // 绘制雷达背景
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(radarX, radarY, radarSize, radarSize, backgroundColor.get());
        Renderer2D.COLOR.render();

        // 绘制雷达边框
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.line(radarX, radarY, radarX + radarSize, radarY, outlineColor.get());
        Renderer2D.COLOR.line(radarX + radarSize, radarY, radarX + radarSize, radarY + radarSize, outlineColor.get());
        Renderer2D.COLOR.line(radarX + radarSize, radarY + radarSize, radarX, radarY + radarSize, outlineColor.get());
        Renderer2D.COLOR.line(radarX, radarY + radarSize, radarX, radarY, outlineColor.get());
        Renderer2D.COLOR.render();

        // 绘制雷达十字线
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.line(centerX, radarY, centerX, radarY + radarSize, outlineColor.get());
        Renderer2D.COLOR.line(radarX, centerY, radarX + radarSize, centerY, outlineColor.get());
        Renderer2D.COLOR.render();

        // 更新闪烁计时
        updateFlicker();

        float yaw = mc.player.getYaw();

        for (PlayerEntity player : players) {
            // 重新获取判定，确保逻辑一致
            boolean isTeam = isTeammate(player);

            // 二次检查显示设置 (处理 show-friends)
            if (isTeam && !showFriends.get()) continue;

            // 计算玩家相对于自己的位置
            double dx = player.getX() - mc.player.getX();
            double dz = player.getZ() - mc.player.getZ();

            // 计算角度
            double angle = Math.toDegrees(Math.atan2(dx, -dz)) - yaw + 90;
            double rad = Math.toRadians(angle);

            // 计算距离 (UI缩放)
            double dist = Math.sqrt(dx * dx + dz * dz);
            double scale = Math.min(dist, range.get()) / range.get(); // 限制在圆内
            
            // 计算指示器坐标
            int indicatorX = (int) (centerX + Math.cos(rad) * radius * scale);
            int indicatorY = (int) (centerY + Math.sin(rad) * radius * scale);

            // --- 颜色判断逻辑 ---
            // 这里的顺序至关重要：自己 > 队友 > 敌人(血量/默认)
            SettingColor renderColor;

            if (player == mc.player) {
                renderColor = ownColor.get();
            } 
            else if (isTeam) {
                // 【核心修复】只要判定为队友，直接赋值蓝色，绝对不执行血量颜色逻辑
                renderColor = friendColor.get();
            } 
            else {
                // 敌人逻辑
                if (healthColors.get()) {
                    double hpVal = getCachedHealth(player);
                    Color baseColor;

                    if (hpVal <= 5.0) baseColor = CRITICAL_HP;
                    else if (hpVal <= 15.0) baseColor = LOW_HP;
                    else if (hpVal <= 30.0) baseColor = MEDIUM_HP;
                    else baseColor = HIGH_HP; // 满血通常是绿色

                    // 闪烁处理：仅当 <= 5 HP 且开启闪烁时
                    if (hpVal <= 5.0 && hpFlicker.get()) {
                        int alpha = getFlickerAlpha();
                        renderColor = new SettingColor(baseColor.r, baseColor.g, baseColor.b, alpha);
                    } else {
                        renderColor = new SettingColor(baseColor.r, baseColor.g, baseColor.b, 255);
                    }
                } else {
                    renderColor = playerColor.get();
                }
            }

            // 绘制点
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(indicatorX - 2, indicatorY - 2, 4, 4, renderColor);
            Renderer2D.COLOR.render();
        }
    }

    /**
     * 判断是否为队友 (增强防闪烁版)
     */
    private boolean isTeammate(PlayerEntity p) {
        if (p == null || mc.player == null) return false;
        
        // 1. Meteor Friends (最稳，中键加好友)
        if (Friends.get().isFriend(p)) return true;

        // 2. 原版 Team 判断
        if (mc.player.isTeammate(p)) return true;

        // 3. 记分板队伍颜色判断
        try {
            net.minecraft.scoreboard.AbstractTeam myTeam = mc.player.getScoreboardTeam();
            net.minecraft.scoreboard.AbstractTeam targetTeam = p.getScoreboardTeam();
            if (myTeam != null && targetTeam != null) {
                if (myTeam.getColor() == targetTeam.getColor()) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略异常，继续执行
        }

        return false;
    }

    // 获取缓存的血量
    private double getCachedHealth(PlayerEntity player) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCacheUpdate > HEALTH_CACHE_INTERVAL) {
            updateHealthCache();
        }
        long playerId = player.getUuid().getMostSignificantBits();
        return cachedHealthMap.getOrDefault(playerId, 20.0);
    }

    // 更新血量缓存
    private void updateHealthCache() {
        if (mc.world == null) return;
        cachedHealthMap.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            cachedHealthMap.put(player.getUuid().getMostSignificantBits(), getGateHp(player));
        }
        lastHealthCacheUpdate = System.currentTimeMillis();
    }

    // 获取真实血量 (Tab列表/Scoreboard优先)
    private double getGateHp(PlayerEntity player) {
        if (mc.world != null && mc.world.getScoreboard() != null) {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (obj != null) {
                ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
                ScoreAccess score = sb.getOrCreateScore(holder, obj);
                int points = score.getScore();
                if (points > 0) return points;
            }
        }
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private void updateFlicker() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFlickerUpdate > 50) {
            flickerTick++;
            lastFlickerUpdate = currentTime;
        }
    }

    private int getFlickerAlpha() {
        double sin = Math.sin(flickerTick * 0.4); 
        return (int) (155 + 100 * sin); 
    }
}