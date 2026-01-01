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

    private final Color RED = new Color(255, 55, 55);
    private final Color AMBER = new Color(255, 170, 0);
    private final Color GREEN = new Color(85, 255, 85);
    private final Color WHITE = new Color(255, 255, 255);
    
    private final Color mutableColor = new Color();
    private final Vector3d pos = new Vector3d();
    private final List<Entity> entitiesList = new ArrayList<>();
    private final StringBuilder sb = new StringBuilder();

    public Nametags() {
        super(Categories.Render, "nametags", "完美名字标签 | 支持Tab真血量 | 物品栏+附魔");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entitiesList.clear();
        boolean freecam = Modules.get().isActive(Freecam.class);
        boolean firstPerson = mc.options.getPerspective().isFirstPerson();
        double maxDistSq = Math.pow(maxDistance.get(), 2);

        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType())) continue;
            
            double distSq = PlayerUtils.squaredDistanceToCamera(entity);
            if (distSq > maxDistSq) continue;
            
            if (entity instanceof PlayerEntity p) {
                if (entity == mc.player && ignoreSelf.get() && !freecam && firstPerson) continue;
                if (ignoreFriends.get() && Friends.get().isFriend(p)) continue;
                if (ignoreBots.get() && EntityUtils.getGameMode(p) == null) continue;
            }
            entitiesList.add(entity);
        }
        
        // 修复：使用 mc.getCameraEntity() 替代 mc.cameraEntity
        // 并使用降序排列（远处的先渲染），以解决渲染遮挡问题
        entitiesList.sort((e1, e2) -> Double.compare(e2.distanceTo(mc.getCameraEntity()), e1.distanceTo(mc.getCameraEntity())));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        boolean shadow = Config.get().customFont.get();

        for (Entity entity : entitiesList) {
            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.5, 0);

            if (!NametagUtils.to2D(pos, scale.get())) continue;

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
        String name = Modules.get().get(NameProtect.class).getName(rawName);
            
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
        
        Color healthCol = displayHp <= 6 ? RED : displayHp <= 13 ? AMBER : GREEN;

        float realHp = player.getHealth() + player.getAbsorptionAmount();
        if (espHealthColors.get()) {
            if (realHp < 10f) nameCol = RED;
            else if (realHp <= 20f) nameCol = AMBER;
            else nameCol = GREEN;
        }

        String pingText = "";
        if (displayPing.get()) {
            sb.setLength(0);
            sb.append(" [").append(EntityUtils.getPing(player)).append("ms]");
            pingText = sb.toString();
        }

        String distText = "";
        if (displayDistance.get()) {
            sb.setLength(0);
            sb.append(" ").append(String.format("%.1f", PlayerUtils.distanceToCamera(player))).append("m");
            distText = sb.toString();
        }

        String drText = "";
        if (displayDamageReduction.get()) {
            float base = 10f;
            float reduced = DamageUtils.calculateReductions(base, player, mc.world.getDamageSources().playerAttack(mc.player));
            float pct = MathHelper.clamp(1f - reduced / base, 0f, 1f) * 100f;
            if (pct >= drMinPercent.get()) {
                sb.setLength(0);
                sb.append(" [DR ").append(String.format("%.0f", pct)).append("%]");
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
            int r = (int) MathHelper.clamp(MathHelper.lerp(t, RED.r, AMBER.r), 0, 255);
            int g = (int) MathHelper.clamp(MathHelper.lerp(t, RED.g, AMBER.g), 0, 255);
            int b = (int) MathHelper.clamp(MathHelper.lerp(t, RED.b, AMBER.b), 0, 255);
            suffixColor = mutableColor.set(r, g, b, 255);
        }

        // --- 渲染阶段 ---
        if (vanillaOverhead.get()) {
            sb.setLength(0);
            if (displayGameMode.get()) sb.append(gmText);
            sb.append(name).append(pingText).append(distText);
            if (displayHealth.get()) sb.append(" [HP ").append(displayHp).append("]");
            sb.append(drText);
            String suffixStr = sb.toString();

            double width = text.getWidth(suffixStr, shadow);
            double x = -width / 2;
            double y = -height * 2;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.get());
            Renderer2D.COLOR.render();

            text.beginBig();
            double cx = x;
            if (displayGameMode.get()) cx = text.render(gmText, cx, y, lowHpOverhead ? suffixColor : gamemodeColor.get(), shadow);
            if (!name.isEmpty()) cx = text.render(name, cx, y, nameCol, shadow);
            if (displayPing.get()) cx = text.render(pingText, cx, y, lowHpOverhead ? suffixColor : pingColor.get(), shadow);
            if (displayDistance.get()) cx = text.render(distText, cx, y, lowHpOverhead ? suffixColor : WHITE, shadow);
            
            if (displayHealth.get()) {
                sb.setLength(0);
                sb.append(" [HP ").append(displayHp).append("]");
                cx = text.render(sb.toString(), cx, y, lowHpOverhead ? suffixColor : healthCol, shadow);
            }
            
            if (displayDamageReduction.get() && !drText.isEmpty()) text.render(drText, cx, y, lowHpOverhead ? suffixColor : WHITE, shadow);
            text.end();

            NametagUtils.end();
            return;
        }

        // 标准模式渲染
        sb.setLength(0);
        sb.append(gmText).append(name).append(healthText).append(pingText).append(distText).append(drText);
        double total = text.getWidth(sb.toString(), shadow);
        double half = total / 2;

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-half - 1, -height - 1, total + 2, height + 2, background.get());
        Renderer2D.COLOR.render();

        text.beginBig();
        double x = -half;
        if (displayGameMode.get()) x = text.render(gmText, x, -height, gamemodeColor.get(), shadow);
        x = text.render(name, x, -height, nameCol, shadow);
        if (displayHealth.get()) x = text.render(healthText, x, -height, healthCol, shadow);
        if (displayPing.get()) x = text.render(pingText, x, -height, pingColor.get(), shadow);
        if (displayDistance.get()) x = text.render(distText, x, -height, WHITE, shadow);
        if (displayDamageReduction.get()) text.render(drText, x, -height, WHITE, shadow);
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
        Renderer2D.COLOR.quad(-w/2 -1, -h -1, w +2, h +2, background.get());
        Renderer2D.COLOR.render();

        text.beginBig();
        text.render(name, -w/2, -h, nameColor.get(), shadow);
        if (!countStr.isEmpty()) {
            text.render(countStr, -w/2 + text.getWidth(name, shadow), -h, WHITE, shadow);
        }
        text.end();

        NametagUtils.end();
    }

    private Integer getTrueTabHealth(PlayerEntity player) {
        if (mc.world == null || mc.getNetworkHandler() == null) return null;
        Scoreboard sb = mc.world.getScoreboard();
        if (sb == null) return null;
        
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        if (obj == null) return null;

        ScoreHolder holder = ScoreHolder.fromProfile(player.getGameProfile());
        ScoreAccess score = sb.getOrCreateScore(holder, obj);
        int points = score.getScore();
        if (points <= 0) return null;
        return points;
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
}