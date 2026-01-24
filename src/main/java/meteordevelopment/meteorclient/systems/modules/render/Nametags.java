/*
 * This file is part of Meteor Client.
 * Copyright (c) Meteor Development.
 * Modified by Grok to support encrypted Tab health perfectly.
 * Optimized for performance & Fixed compilation errors.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.*;

public class Nametags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .defaultValue(EntityType.PLAYER, EntityType.ITEM)
        .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale").defaultValue(1.1).min(0.1).build());

    private final Setting<Double> nearDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("near-distance")
        .description("近距离阈值，在此距离内名字标签最大且不透明")
        .defaultValue(2.0)
        .min(0.1)
        .max(100.0)
        .sliderRange(0.1, 10.0)
        .build());

    private final Setting<Double> farDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("far-distance")
        .description("远距离阈值，在此距离外名字标签最小且透明")
        .defaultValue(30.0)
        .min(0.1)
        .max(200.0)
        .sliderRange(1.0, 50.0)
        .build());

    private final Setting<Boolean> vanillaOverhead = sgGeneral.add(new BoolSetting.Builder()
        .name("vanilla-overhead")
        .description("在原版名字标签上方居中追加信息")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> distanceScaling = sgGeneral.add(new BoolSetting.Builder()
        .name("distance-scaling")
        .description("是否随距离缩放标签大小，关闭以接近原版效果")
        .defaultValue(false)
        .build());

    public final Setting<Boolean> hideVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-vanilla")
        .description("忽略/隐藏原版名字标签")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self").defaultValue(true).build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends").defaultValue(false).build());

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-bots").defaultValue(true).build());

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("名字标签的最大显示距离")
        .defaultValue(100)
        .min(1)
        .max(1000)
        .sliderRange(1, 200)
        .build());

    // Players
    private final Setting<Boolean> displayHealth = sgPlayers.add(new BoolSetting.Builder()
        .name("health").defaultValue(true).build());

    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder()
        .name("gamemode").defaultValue(false).build());

    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("distance").defaultValue(false).build());

    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder()
        .name("ping").defaultValue(true).build());

    private final Setting<Boolean> espHealthColors = sgPlayers.add(new BoolSetting.Builder()
        .name("esp-health-colors")
        .description("根据生命值动态改变名字/文本颜色，<=20橙色，<10红色")
        .defaultValue(true)
        .build());

    private final Setting<Integer> maxNameLength = sgPlayers.add(new IntSetting.Builder()
        .name("max-name-length")
        .description("限制玩家名称的最大显示长度，设置为0时完全隐藏名称")
        .defaultValue(10)
        .min(0)
        .max(50)
        .build());

    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder()
        .name("items").defaultValue(true).build());

    private final Setting<Boolean> displayEnchants = sgPlayers.add(new BoolSetting.Builder()
        .name("display-enchants")
        .defaultValue(true)
        .visible(displayItems::get)
        .build());

    private final Setting<Boolean> displayTeamIcon = sgPlayers.add(new BoolSetting.Builder()
        .name("team-icon")
        .description("显示玩家队伍图标")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> displayDamageReduction = sgItems.add(new BoolSetting.Builder()
        .name("damage-reduction")
        .description("显示基于装备的伤害减免百分比")
        .defaultValue(true)
        .build());

    private final Setting<Integer> drMinPercent = sgItems.add(new IntSetting.Builder()
        .name("dr-min-percent")
        .description("仅当减免百分比≥该值时显示")
        .defaultValue(10)
        .min(0)
        .max(100)
        .visible(displayDamageReduction::get)
        .build());

    // Items
    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder()
        .name("show-count").defaultValue(true).build());

    // Render
    private final Setting<SettingColor> background = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build());

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .defaultValue(new SettingColor())
        .build());

    private final Setting<SettingColor> pingColor = sgRender.add(new ColorSetting.Builder()
        .name("ping-color")
        .defaultValue(new SettingColor(20, 170, 170))
        .visible(displayPing::get)
        .build());

    private final Setting<SettingColor> gamemodeColor = sgRender.add(new ColorSetting.Builder()
        .name("gamemode-color")
        .defaultValue(new SettingColor(232, 185, 35))
        .visible(displayGameMode::get)
        .build());

    private final Color LOW_HP = new Color(200, 80, 80);
    private final Color MEDIUM_HP = new Color(255, 180, 60);
    private final Color HIGH_HP = new Color(100, 200, 100);
    private final Color WHITE = new Color(255, 255, 255);
    private final Color BLACK = new Color(0, 0, 0); // 新增黑色常量
    
    private final Color mutableColor = new Color();
    private final Vector3d pos = new Vector3d();
    private final List<Entity> entitiesList = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();
    
    // 新增：透明度杠杆实例变量
    private int bgAlpha;
    private int textAlpha;

    public Nametags() {
        super(Categories.Render, "nametags", "完美名字标签 | 支持Tab真血量 | 物品栏+附魔");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entitiesList.clear();
        boolean freecam = Modules.get().isActive(Freecam.class);
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        double maxDist = maxDistance.get();
        double maxDistSq = maxDist * maxDist;

        // 预缓存设置值以减少重复get()调用
        Set<EntityType<?>> entityTypes = entities.get();
        boolean ignoreSelfFlag = ignoreSelf.get();
        boolean ignoreFriendsFlag = ignoreFriends.get();
        boolean ignoreBotsFlag = ignoreBots.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!entityTypes.contains(entity.getType())) continue;
            
            double distSq = PlayerUtils.squaredDistanceToCamera(entity);
            if (distSq > maxDistSq) continue;
            
            if (entity instanceof PlayerEntity p) {
                if (entity == mc.player && ignoreSelfFlag && !freecam && firstPerson) continue;
                if (ignoreFriendsFlag && Friends.get().isFriend(p)) continue;
                if (ignoreBotsFlag && EntityUtils.getGameMode(p) == null) continue;
            }
            entitiesList.add(entity);
        }
        
        // 使用更高效的排序：降序排列（远处的先渲染）
        entitiesList.sort((e1, e2) -> {
            double d1 = PlayerUtils.squaredDistanceToCamera(e1);
            double d2 = PlayerUtils.squaredDistanceToCamera(e2);
            return Double.compare(d2, d1); // 降序：远到近
        });
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        boolean shadow = Config.get().customFont.get();

        for (Entity entity : entitiesList) {
            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.5, 0);

            /* =====  距离杠杆 + 透明度杠杆  ===== */
            double dist = PlayerUtils.distanceToCamera(entity);      // 米
            float distFade = smoothstep(nearDistance.get().floatValue(), farDistance.get().floatValue(), (float) dist);    // 0~1，根据设置的距离阈值调整
            float finalScale = (float) (scale.get() * (1 - distFade * 0.5f)); // 远→小，最小 0.5x
            finalScale = MathHelper.clamp(finalScale, 0.5f, 10f);

            bgAlpha   = (int) MathHelper.lerp(distFade, 255, 60); // 近 255 远 60
            textAlpha = bgAlpha;                                   // 文字同步

            if (!NametagUtils.to2D(pos, finalScale)) continue;   // 用新 scale

            if (entity instanceof PlayerEntity player) {
                renderPlayer(event, player, shadow);
            } else if (entity instanceof ItemEntity item) {
                renderItem(item.getStack(), shadow);
            }
        }
    }

    private void renderPlayer(Render2DEvent event, PlayerEntity player, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // --- 数据准备阶段 ---
        String gmText = "";
        if (displayGameMode.get()) {
            GameMode gm = EntityUtils.getGameMode(player);
            gmText = gm == null ? "[BOT] " : switch (gm) {
                case SPECTATOR -> "[Sp] ";
                case SURVIVAL -> "[S] ";
                case CREATIVE -> "[C] ";
                case ADVENTURE -> "[A] ";
            };
        }

        String rawName = player.getName().getString();
        String name = player == mc.player
            ? Modules.get().get(NameProtect.class).getName(rawName)
            : rawName;
            
        if (maxNameLength.get() > 0 && name.length() > maxNameLength.get()) {
            name = name.substring(0, maxNameLength.get()) + "...";
        } else if (maxNameLength.get() == 0) {
            name = "";
        }
        
        Color nameCol = PlayerUtils.getPlayerColor(player, nameColor.get());

        Integer tabHealth = getTrueTabHealth(player);
        int displayHp = tabHealth != null ? tabHealth : 100;
        
        sb.setLength(0);
        sb.append(" ").append(displayHp);
        String healthText = sb.toString();
        
        // 修改：血量颜色固定为黑色
        Color healthCol = BLACK;

        float realHp = player.getHealth() + player.getAbsorptionAmount();
        if (espHealthColors.get()) {
            if (realHp < 10f) nameCol = LOW_HP;
            else if (realHp <= 20f) nameCol = MEDIUM_HP;
            else nameCol = HIGH_HP;
        }

        // 优化：缓存常用的设置值
        boolean displayPingFlag = displayPing.get();
        boolean displayDistanceFlag = displayDistance.get();
        
        String pingText = "";
        if (displayPingFlag) {
            sb.setLength(0);
            sb.append(" [").append(EntityUtils.getPing(player)).append("ms]");
            pingText = sb.toString();
        }

        String distText = "";
        if (displayDistanceFlag) {
            sb.setLength(0);
            sb.append(" ").append(String.format("%.1f", PlayerUtils.distanceToCamera(player))).append("m");
            distText = sb.toString();
        }

        // 优化：缓存设置值减少重复调用
        boolean displayDrFlag = displayDamageReduction.get();
        int drMinPercentVal = drMinPercent.get();
        
        String drText = "";
        if (displayDrFlag) {
            float base = 10f;
            float reduced = DamageUtils.calculateReductions(base, player, mc.world.getDamageSources().playerAttack(mc.player));
            float pct = MathHelper.clamp(1f - reduced / base, 0f, 1f) * 100f;
            if (pct >= drMinPercentVal) {
                sb.setLength(0);
                sb.append(" [DR ").append((int)pct).append("%]");
                drText = sb.toString();
            }
        }

        // --- 颜色计算 ---
        double height = text.getHeight(shadow);
        double hpValOverhead = tabHealth != null ? tabHealth.doubleValue() : (player.getHealth() + player.getAbsorptionAmount());
        boolean lowHpOverhead = hpValOverhead < 20.0;
        
        Color suffixColor = WHITE;
        if (lowHpOverhead) {
            double t = MathHelper.clamp(hpValOverhead / 20.0, 0.0, 1.0);
            int r = (int) MathHelper.clamp(MathHelper.lerp(t, LOW_HP.r, MEDIUM_HP.r), 0, 255);
            int g = (int) MathHelper.clamp(MathHelper.lerp(t, LOW_HP.g, MEDIUM_HP.g), 0, 255);
            int b = (int) MathHelper.clamp(MathHelper.lerp(t, LOW_HP.b, MEDIUM_HP.b), 0, 255);
            suffixColor = mutableColor.set(r, g, b, 255);
        }

        // --- 渲染阶段 ---
        if (vanillaOverhead.get()) {
            sb.setLength(0);
            if (displayGameMode.get()) sb.append(gmText);
            
            // 渲染队伍图标
            Team playerTeam = player.getScoreboardTeam();
            if (displayTeamIcon.get() && playerTeam != null) {
                String teamPrefix = playerTeam.getPrefix().getString();
                String teamSuffix = playerTeam.getSuffix().getString();
                if (!teamPrefix.isEmpty()) {
                    sb.append(teamPrefix);
                } else if (!teamSuffix.isEmpty()) {
                    sb.append(teamSuffix);
                } else {
                    sb.append("[T] "); // 默认队伍图标
                }
            }
            
            sb.append(name).append(pingText).append(distText);
            if (displayHealth.get()) sb.append(" [HP ").append(displayHp).append("]");
            sb.append(drText);
            String suffixStr = sb.toString();


            double width = text.getWidth(suffixStr, shadow);
            double x = -width / 2;
            double y = -height * 2;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2,
                    new Color(newAlpha(background.get().getPacked(), bgAlpha)));
            Renderer2D.COLOR.render();

            text.beginBig();
            double cx = x;
            if (displayGameMode.get()) cx = text.render(gmText, cx, y,
                    new Color(newAlpha((lowHpOverhead ? suffixColor : gamemodeColor.get()).getPacked(), textAlpha)),
                    shadow);
            
            // 渲染队伍图标
            if (displayTeamIcon.get() && playerTeam != null) {
                String teamPrefix = playerTeam.getPrefix().getString();
                String teamSuffix = playerTeam.getSuffix().getString();
                Integer colorValue = playerTeam.getColor().getColorValue();
                Color teamColor = colorValue != null ? new Color(colorValue) : WHITE;
                
                if (!teamPrefix.isEmpty()) {
                    cx = text.render(teamPrefix, cx, y,
                            new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                            shadow);
                } else if (!teamSuffix.isEmpty()) {
                    cx = text.render(teamSuffix, cx, y,
                            new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                            shadow);
                } else {
                    cx = text.render("[T] ", cx, y,
                            new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                            shadow);
                }
            }
            
            if (!name.isEmpty()) cx = text.render(name, cx, y,
                    new Color(newAlpha(nameCol.getPacked(), textAlpha)),
                    shadow);
            if (displayPing.get()) cx = text.render(pingText, cx, y,
                    new Color(newAlpha((lowHpOverhead ? suffixColor : pingColor.get()).getPacked(), textAlpha)),
                    shadow);
            if (displayDistance.get()) cx = text.render(distText, cx, y,
                    new Color(newAlpha((lowHpOverhead ? suffixColor : WHITE).getPacked(), textAlpha)),
                    shadow);
            
            if (displayHealth.get()) {
                sb.setLength(0);
                sb.append(" [HP ").append(displayHp).append("]");
                // 修改：强制使用 BLACK 颜色渲染血量，忽略低血量变色逻辑
                cx = text.render(sb.toString(), cx, y,
                        new Color(newAlpha(BLACK.getPacked(), textAlpha)),
                        shadow);
            }
            
            if (displayDamageReduction.get() && !drText.isEmpty()) text.render(drText, cx, y,
                    new Color(newAlpha(healthCol.getPacked(), textAlpha)),
                    shadow);
            text.end();

            NametagUtils.end();
            return;
        }

        // 标准模式渲染
        Team playerTeam = player.getScoreboardTeam();
        
        sb.setLength(0);
        if (displayGameMode.get()) sb.append(gmText);
        
        // 添加队伍图标到字符串
        if (displayTeamIcon.get() && playerTeam != null) {
            String teamPrefix = playerTeam.getPrefix().getString();
            String teamSuffix = playerTeam.getSuffix().getString();
            if (!teamPrefix.isEmpty()) {
                sb.append(teamPrefix);
            } else if (!teamSuffix.isEmpty()) {
                sb.append(teamSuffix);
            } else {
                sb.append("[T] ");
            }
        }
        

        sb.append(name).append(healthText).append(pingText).append(distText).append(drText);
        double total = text.getWidth(sb.toString(), shadow);
        double half = total / 2;

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-half - 1, -height - 1, total + 2, height + 2,
                new Color(newAlpha(background.get().getPacked(), bgAlpha)));
        Renderer2D.COLOR.render();

        text.beginBig();
        double x = -half;
        if (displayGameMode.get()) x = text.render(gmText, x, -height,
                new Color(newAlpha(gamemodeColor.get().getPacked(), textAlpha)),
                shadow);
        
        // 渲染队伍图标
        if (displayTeamIcon.get() && playerTeam != null) {
            String teamPrefix = playerTeam.getPrefix().getString();
            String teamSuffix = playerTeam.getSuffix().getString();
            Integer colorValue = playerTeam.getColor().getColorValue();
            Color teamColor = colorValue != null ? new Color(colorValue) : WHITE;
            
            if (!teamPrefix.isEmpty()) {
                x = text.render(teamPrefix, x, -height,
                        new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                        shadow);
            } else if (!teamSuffix.isEmpty()) {
                x = text.render(teamSuffix, x, -height,
                        new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                        shadow);
            } else {
                x = text.render("[T] ", x, -height,
                        new Color(newAlpha(teamColor.getPacked(), textAlpha)),
                        shadow);
            }
        }
        
        x = text.render(name, x, -height,
                new Color(newAlpha(nameCol.getPacked(), textAlpha)),
                shadow);
        // 修改：强制使用 BLACK (healthCol已设置为BLACK)
        if (displayHealth.get()) x = text.render(healthText, x, -height,
                new Color(newAlpha(healthCol.getPacked(), textAlpha)),
                shadow);
        if (displayPing.get()) x = text.render(pingText, x, -height,
                new Color(newAlpha(pingColor.get().getPacked(), textAlpha)),
                shadow);
        if (displayDistance.get()) x = text.render(distText, x, -height,
                new Color(newAlpha(WHITE.getPacked(), textAlpha)),
                shadow);
        if (displayDamageReduction.get()) text.render(drText, x, -height,
                new Color(newAlpha(healthCol.getPacked(), textAlpha)),
                shadow);
        text.end();

        if (displayItems.get()) {
            renderItemsAndEnchants(event, player, shadow, height);
        }

        NametagUtils.end();
    }
    
    private void renderItemsAndEnchants(Render2DEvent event, PlayerEntity player, boolean shadow, double height) {
        TextRenderer text = TextRenderer.get();
        double itemY = -height - 38;
        double itemX = -48;

        for (int i = 0; i < 6; i++) {
            ItemStack stack = getItem(player, i);
            RenderUtils.drawItem(event.drawContext, stack, (int) itemX, (int) itemY, 2, true);

            if (displayEnchants.get() && !stack.isEmpty()) {
                ItemEnchantmentsComponent ench = EnchantmentHelper.getEnchantments(stack);
                text.begin(0.9, false, true);
                double ex = itemX + 16;
                double ey = itemY;
                
                Set<RegistryEntry<Enchantment>> enchantments = ench.getEnchantments();
                for (RegistryEntry<Enchantment> e : enchantments) {
                    sb.setLength(0);
                    sb.append(Utils.getEnchantSimpleName(e, 3)).append(" ").append(ench.getLevel(e));
                    String line = sb.toString();
                    
                    text.render(line, ex - text.getWidth(line, shadow), ey, WHITE, shadow);
                    ey += text.getHeight(shadow);
                }
                text.end();
            }
            itemX += 18;
        }
    }

    private void renderItem(ItemStack stack, boolean shadow) {
        if (stack.isEmpty()) return;
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = Names.get(stack);
        
        sb.setLength(0);
        if (itemCount.get() && stack.getCount() > 1) {
            sb.append(" x").append(stack.getCount());
        }
        String countStr = sb.toString();


        double w = text.getWidth(name + countStr, shadow);
        double h = text.getHeight(shadow);

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-w/2 -1, -h -1, w +2, h +2,
                new Color(newAlpha(background.get().getPacked(), bgAlpha)));
        Renderer2D.COLOR.render();

        text.beginBig();
        text.render(name, -w/2, -h,
                new Color(newAlpha(nameColor.get().getPacked(), textAlpha)),
                shadow);
        if (!countStr.isEmpty()) {
            text.render(countStr, -w/2 + text.getWidth(name, shadow), -h,
                    new Color(newAlpha(WHITE.getPacked(), textAlpha)),
                    shadow);
        }
        text.end();

        NametagUtils.end();
    }

    private Integer getTrueTabHealth(PlayerEntity player) {
        if (mc.world == null || mc.getNetworkHandler() == null) return null;
        
        try {
            Scoreboard sb = mc.world.getScoreboard();
            if (sb == null) return null;
            
            // 尝试获取 LIST 槽位
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            
            // Lunar 修复：如果 LIST 没数据，尝试 BELOW_NAME (头顶血量)
            if (obj == null) {
                obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            }
            
            if (obj == null) return null;

            ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
            ScoreAccess score = sb.getOrCreateScore(holder, obj);
            int points = score.getScore();
            
            // 过滤无效数值
            if (points <= 0 && obj.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) return null;
            return points;
        } catch (Exception e) {
            // 发生任何错误都返回 null，保证名字标签至少能显示出来，不要报错
            return null;
        }
    }

    private ItemStack getItem(PlayerEntity p, int i) {
        return switch (i) {
            case 0 -> p.getMainHandStack();
            case 1 -> p.getEquippedStack(EquipmentSlot.HEAD);
            case 2 -> p.getEquippedStack(EquipmentSlot.CHEST);
            case 3 -> p.getEquippedStack(EquipmentSlot.LEGS);
            case 4 -> p.getEquippedStack(EquipmentSlot.FEET);
            case 5 -> p.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    public boolean excludeBots() {
        return ignoreBots.get();
    }

    public boolean playerNametags() {
        return isActive() && entities.get().contains(EntityType.PLAYER);
    }

    /* ---------- 新增：两条曲线 ---------- */
    private static float smoothstep(float edge0, float edge1, float x) {
        x = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return x * x * (3 - 2 * x);
    }
    private static int newAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}