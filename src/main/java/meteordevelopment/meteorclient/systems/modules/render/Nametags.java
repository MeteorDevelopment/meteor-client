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
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.*;

public class Nametags extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgRender  = settings.createGroup("Render");

    // General
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select entities to draw nametags on.")
        .defaultValue(EntityType.PLAYER)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the nametag.")
        .defaultValue(1.1)
        .min(0.1)
        .max(1.5)
        .sliderRange(0.1, 1.5)
        .build()
    );

    private final Setting<Boolean> inverseDistanceScale = sgGeneral.add(new BoolSetting.Builder()
        .name("inverse-distance-scale")
        .description("Nametags are smaller when close and larger when far.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> hideVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-vanilla")
        .description("Hides vanilla nametags.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Ignore yourself when in third person or freecam.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignore rendering nametags for friends.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreBots = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-bots")
        .description("Only render non-bot nametags.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("culling")
        .description("Only render a certain number of nametags at a certain distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("culling-range")
        .description("Only render nametags within this distance of your player.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .visible(culling::get)
        .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("culling-count")
        .description("Only render this many nametags.")
        .defaultValue(50)
        .min(1)
        .sliderRange(1, 100)
        .visible(culling::get)
        .build()
    );

    // Players
    private final Setting<Boolean> pvpNametag = sgPlayers.add(new BoolSetting.Builder()
        .name("pvp-nametag")
        .description("Show HP nametag on players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistance = sgPlayers.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Show distance to player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> distanceDecimals = sgPlayers.add(new IntSetting.Builder()
        .name("distance-decimals")
        .defaultValue(1)
        .range(0, 3)
        .visible(showDistance::get)
        .build()
    );

    private final Setting<Boolean> showArmorReduction = sgPlayers.add(new BoolSetting.Builder()
        .name("show-armor-reduction")
        .description("Show armor damage reduction percentage.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> armorReductionThreshold = sgPlayers.add(new IntSetting.Builder()
        .name("reduction-threshold")
        .description("Only show armor reduction when it exceeds this percentage.")
        .defaultValue(65)
        .range(0, 100)
        .sliderRange(0, 100)
        .visible(showArmorReduction::get)
        .build()
    );

    // Render
    private final Setting<SettingColor> background = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The color of the nametag background.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("name-color")
        .description("The color of the nametag names.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Color RED   = new Color(255, 25,  25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color GREEN = new Color(25,  252, 25);
    private final Color CYAN  = new Color(20,  200, 200);
    private final Color GOLD  = new Color(232, 185, 35);

    private final Vector3d pos = new Vector3d();

    private final List<Entity> entityList = new ArrayList<>();
    private final HashMap<Long, Double> cachedHealthMap = new HashMap<>();
    private long lastHealthCacheUpdate = 0;
    private static final long HEALTH_CACHE_INTERVAL = 500;

    public Nametags() {
        super(Categories.Render, "nametags", "Displays HP and armor reduction nametags above players.");
    }

    @Override
    public void onDeactivate() {
        cachedHealthMap.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();

        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        boolean notThirdPerson   = mc.options.getPerspective().isFirstPerson();
        Vec3d cameraPos          = mc.gameRenderer.getCamera().getCameraPos();

        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();
            if (!entities.get().contains(type)) continue;

            if (type == EntityType.PLAYER) {
                if ((ignoreSelf.get() || (freecamNotActive && notThirdPerson))
                    && entity == mc.player) continue;

                if (EntityUtils.getGameMode((PlayerEntity) entity) == null
                    && ignoreBots.get()) continue;

                if (Friends.get().isFriend((PlayerEntity) entity)
                    && ignoreFriends.get()) continue;
            }

            if (!culling.get() || PlayerUtils.isWithinCamera(entity, maxCullRange.get())) {
                entityList.add(entity);
            }
        }

        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }

    // ── 血量缓存 ──────────────────────────────────────────────────────────────

    private double getCachedHealth(PlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now - lastHealthCacheUpdate > HEALTH_CACHE_INTERVAL) updateHealthCache();
        long id = player.getUuid().getMostSignificantBits();
        return cachedHealthMap.getOrDefault(id, 20.0);
    }

    private void updateHealthCache() {
        if (mc.world == null) return;
        cachedHealthMap.clear();
        for (PlayerEntity p : mc.world.getPlayers()) {
            cachedHealthMap.put(p.getUuid().getMostSignificantBits(), getGateHp(p));
        }
        lastHealthCacheUpdate = System.currentTimeMillis();
    }

    private double getGateHp(PlayerEntity player) {
        if (mc.world != null && mc.world.getScoreboard() != null) {
            Scoreboard sb  = mc.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (obj != null) {
                try {
                    ReadableScoreboardScore score = sb.getScore(player, obj);
                    if (score != null && score.getScore() > 0) return score.getScore();
                } catch (Exception ignored) {}
            }
        }
        return player.getHealth() + player.getAbsorptionAmount();
    }

    // ── 护甲减伤计算 ──────────────────────────────────────────────────────────

    private double calcDamageReduction(PlayerEntity player) {
        double armor     = player.getArmor();
        double toughness = player.getAttributeValue(
            net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);

        double refDamage = 1.0;
        double armorEff  = Math.max(armor / 5.0,
                           Math.min(armor - refDamage * (4.0 / (toughness + 8.0)), 20.0));
        double armorReduction = armorEff / 25.0;

        int totalProtLevel = 0;
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                                   EquipmentSlot.LEGS, EquipmentSlot.FEET };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants == null) continue;

            for (var entry : enchants.getEnchantmentEntries()) {
                RegistryKey<Enchantment> key = entry.getKey().getKey().orElse(null);
                int lvl = entry.getIntValue();
                if (key == null) continue;

                if (key.equals(Enchantments.PROTECTION))                 totalProtLevel += lvl;
                else if (key.equals(Enchantments.BLAST_PROTECTION))      totalProtLevel += lvl * 2;
                else if (key.equals(Enchantments.FIRE_PROTECTION))       totalProtLevel += lvl * 2;
                else if (key.equals(Enchantments.PROJECTILE_PROTECTION)) totalProtLevel += (int)(lvl * 1.5);
            }
        }

        double enchantReduction = Math.min(totalProtLevel * 0.04, 0.8);
        double total = 1.0 - (1.0 - armorReduction) * (1.0 - enchantReduction);
        return Math.min(total, 0.95);
    }

    // ── 渲染 ──────────────────────────────────────────────────────────────────

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count  = getRenderCount();
        boolean shadow = Config.get().customFont.get();

        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);

            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);

            if (inverseDistanceScale.get()) {
                Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
                double dx = cameraPos.x - entity.getX();
                double dy = cameraPos.y - entity.getY();
                double dz = cameraPos.z - entity.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                // 最终渲染尺寸不超过 scale 上限 1.5
                double invScale = scale.get() * MathHelper.clamp(0.4 + dist * 0.06, 0.4, 1.5 / scale.get());
                if (NametagUtils.to2D(pos, invScale, false)) {
                    if (entity.getType() == EntityType.PLAYER)
                        renderNametagPlayer(event, (PlayerEntity) entity, shadow);
                }
            } else {
                if (NametagUtils.to2D(pos, scale.get())) {
                    if (entity.getType() == EntityType.PLAYER)
                        renderNametagPlayer(event, (PlayerEntity) entity, shadow);
                }
            }
        }
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        return MathHelper.clamp(count, 0, entityList.size());
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private double getHeight(Entity entity) {
        return entity.getEyeHeight(entity.getPose()) + 0.5;
    }

    private void renderNametagPlayer(Render2DEvent event, PlayerEntity player, boolean shadow) {
        if (!pvpNametag.get()) return;

        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos, event.drawContext);

        // ── 血量 ──
        double hp     = getCachedHealth(player);
        int    health = Math.round((float) hp);

        Color hpColor;
        if (hp >= 99)      hpColor = GREEN;
        else if (hp <= 6)  hpColor = RED;
        else if (hp <= 12) hpColor = AMBER;
        else               hpColor = GREEN;

        String hpText = String.valueOf(health);

        // ── 减伤百分比 ──
        boolean showReduction  = false;
        String  reductionText  = "";
        Color   reductionColor = CYAN;

        if (showArmorReduction.get()) {
            double reduction    = calcDamageReduction(player);
            int    reductionPct = (int) Math.round(reduction * 100);
            if (reductionPct > armorReductionThreshold.get()) {
                showReduction = true;
                reductionText = reductionPct + "%";
                reductionColor = reductionPct >= 80 ? GOLD : CYAN;
            }
        }

        // ── 距离 ──
        boolean showDist = showDistance.get()
            && (player != mc.getCameraEntity() || Modules.get().isActive(Freecam.class));
        String distText = "";

        if (showDist) {
            double s    = Math.pow(10, distanceDecimals.get());
            double dist = Math.round(PlayerUtils.distanceToCamera(player) * s) / s;
            distText = dist + "m";
        }

        // ── 计算布局 ──
        double lineH = text.getHeight(shadow);
        double hpW   = text.getWidth(hpText, shadow);
        double redW  = showReduction ? text.getWidth(reductionText, shadow) : 0;
        double distW = showDist      ? text.getWidth(distText, shadow)      : 0;

        double maxW  = Math.max(hpW, Math.max(redW, distW));
        double halfW = maxW / 2;

        int    lines  = 1 + (showReduction ? 1 : 0) + (showDist ? 1 : 0);
        double totalH = lineH * lines;

        drawBg(-halfW, -totalH, maxW, totalH);

        text.beginBig();

        double y = -totalH;

        // 第1行：血量
        text.render(hpText, -hpW / 2, y, hpColor, shadow);
        y += lineH;

        // 第2行（可选）：减伤百分比
        if (showReduction) {
            text.render(reductionText, -redW / 2, y, reductionColor, shadow);
            y += lineH;
        }

        // 第3行（可选）：距离
        if (showDist) {
            text.render(distText, -distW / 2, y, EntityUtils.getColorFromDistance(player), shadow);
        }

        text.end();
        NametagUtils.end(event.drawContext);
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.get());
        Renderer2D.COLOR.render();
    }

    // ── 枚举 ──────────────────────────────────────────────────────────────────

    public enum Position    { Above, OnTop }
    public enum Durability  { None, Total, Percentage }
    public enum DistanceColorMode { Gradient, Flat }

    // ── 对外接口 ──────────────────────────────────────────────────────────────

    public boolean excludeBots() { return ignoreBots.get(); }

    public boolean playerNametags() {
        return isActive() && entities.get().contains(EntityType.PLAYER);
    }
}