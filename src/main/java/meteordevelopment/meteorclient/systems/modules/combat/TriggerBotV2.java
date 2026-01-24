package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.UltimateSprint;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;

import java.util.Random;
import java.util.Set;

public class TriggerBotV2 extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLogic = settings.createGroup("Logic (判定)");
    private final SettingGroup sgTiming = settings.createGroup("Timing (延迟/冷却)");

    private final Random random = new Random();

    // ==================== General ====================
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities").defaultValue(Set.of(EntityType.PLAYER)).build());

    private final Setting<Boolean> checkTeams = sgGeneral.add(new BoolSetting.Builder()
            .name("check-teams").defaultValue(true).build());

    // ==================== Logic ====================
    private final Setting<Double> hitChance = sgLogic.add(new DoubleSetting.Builder()
            .name("hit-chance").defaultValue(90.0).min(1.0).max(100.0).build());

    private final Setting<Boolean> smartAirSwing = sgLogic.add(new BoolSetting.Builder()
            .name("smart-air-swing").defaultValue(true).build());

    private final Setting<Double> airSwingMin = sgLogic.add(new DoubleSetting.Builder()
            .name("air-swing-min").defaultValue(4.5).visible(smartAirSwing::get).build());

    private final Setting<Double> airSwingMax = sgLogic.add(new DoubleSetting.Builder()
            .name("air-swing-max").defaultValue(5.5).visible(smartAirSwing::get).build());

    private final Setting<Integer> comboHoverMin = sgTiming.add(new IntSetting.Builder()
            .name("combo-hover-min").defaultValue(1).min(0).build());

    private final Setting<Integer> comboHoverMax = sgTiming.add(new IntSetting.Builder()
            .name("combo-hover-max").defaultValue(2).min(0).build());

    private final Setting<Double> critThreshold = sgTiming.add(new DoubleSetting.Builder()
            .name("crit-threshold").defaultValue(0.72).build());

    private final Setting<Double> firstHitThreshold = sgTiming.add(new DoubleSetting.Builder()
            .name("first-hit-threshold").defaultValue(0.80).build());

    private final Setting<Double> comboMinThreshold = sgTiming.add(new DoubleSetting.Builder()
            .name("combo-min").defaultValue(0.875).build());

    private final Setting<Double> comboMaxThreshold = sgTiming.add(new DoubleSetting.Builder()
            .name("combo-max").defaultValue(1.0).build());

    // ==================== State (Static) ====================
    private static Entity lockedTarget = null;

    // ==================== State (Instance) ====================
    private boolean isFirstAttack = true;
    private Entity lastTickTarget = null;
    private int hoverTickCounter = 0;
    private int comboRequiredHover = -1;
    private boolean hasClickedThisTick = false;

    public TriggerBotV2() {
        super(Categories.Combat, "trigger-bot-v2", "Instant activation on first hit.");
    }

    @Override
    public void onActivate() {
        resetState();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        isFirstAttack = true;
        lockedTarget = null;
        lastTickTarget = null;
        hoverTickCounter = 0;
        comboRequiredHover = -1;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPreTick(TickEvent.Pre event) {
        hasClickedThisTick = false;
        if (mc.player == null || mc.world == null || mc.player.isUsingItem())
            return;

        // [删除] 移除了 isPanicTick，因为这个逻辑会导致命中率失效

        // --- Layer 2: Smart Air Swing (仅首刀) ---
        if (isFirstAttack && smartAirSwing.get()) {
            double range = airSwingMin.get() + (random.nextDouble() * (airSwingMax.get() - airSwingMin.get()));
            if (getNearbyEnemiesCount(range) == 0) {
                doLeftClick(false);
                isFirstAttack = false;
                return;
            }
        }

        // --- Layer 3: Combat Logic ---
        Entity currentTarget = getCrosshairTarget();

        // 持续瞄准计数器
        if (currentTarget == null || currentTarget != lastTickTarget) {
            hoverTickCounter = 0;
        } else {
            hoverTickCounter++;
        }

        if (currentTarget == null) {
            if (lockedTarget != null && !lockedTarget.isAlive())
                lockedTarget = null;
            lastTickTarget = null;
            return;
        }

        boolean shouldCrit = canCrit();
        boolean hoverReady = false;

        if (isFirstAttack) {
            hoverReady = true;
        } else {
            if (currentTarget != lockedTarget) {
                lastTickTarget = currentTarget;
                return;
            }
            if (comboRequiredHover == -1) {
                comboRequiredHover = comboHoverMin.get()
                        + random.nextInt(Math.max(1, comboHoverMax.get() - comboHoverMin.get() + 1));
            }
            hoverReady = (hoverTickCounter >= comboRequiredHover);
        }

        lastTickTarget = currentTarget;

        // --- 最终判定执行 ---
        boolean readyToAttack = false;

        if (hoverReady) {
            float progress = mc.player.getAttackCooldownProgress(0.5f);
            boolean cooldownMet = false;

            // 1. 先检查冷却是否达标
            if (shouldCrit) {
                cooldownMet = (progress >= critThreshold.get());
            } else if (isFirstAttack) {
                cooldownMet = (progress >= firstHitThreshold.get());
            } else {
                double rndThreshold = comboMinThreshold.get()
                        + (random.nextDouble() * (comboMaxThreshold.get() - comboMinThreshold.get()));
                cooldownMet = (progress >= rndThreshold);
            }

            // 2. 如果冷却达标，再计算命中率 (Hit Chance)
            if (cooldownMet) {
                // 生成一个 0-100 的随机数
                double roll = random.nextDouble() * 100.0;

                // 只有当 随机数 <= 设置的命中率 时，才允许攻击
                // 例如设置 90%，如果 roll 出 95，则不攻击（模拟失误/反应慢）
                if (roll <= hitChance.get()) {
                    readyToAttack = true;
                }
            }
        }

        if (readyToAttack) {
            if (isFirstAttack)
                lockedTarget = currentTarget;
            doLeftClick(shouldCrit);
            postAttackProcessing();
        }
    }

    private void postAttackProcessing() {
        isFirstAttack = false;
        hoverTickCounter = 0;
        comboRequiredHover = -1;
    }

    private void doLeftClick(boolean requestCrit) {
        if (hasClickedThisTick)
            return;
        if (requestCrit) {
            UltimateSprint.requestCritUnsprint(() -> ((MinecraftClientAccessor) mc).meteor$leftClick());
        } else {
            ((MinecraftClientAccessor) mc).meteor$leftClick();
        }
        hasClickedThisTick = true;
    }

    private Entity getCrosshairTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity entity = ehr.getEntity();
            if (isValid(entity))
                return entity;
        }
        return null;
    }

    private boolean isValid(Entity e) {
        if (e == null || !e.isAlive() || e == mc.player)
            return false;
        if (e instanceof LivingEntity le && le.getHealth() <= 0)
            return false;
        if (!entities.get().contains(e.getType()))
            return false;
        if (e instanceof PlayerEntity p) {
            if (p.isCreative() || p.isSpectator())
                return false;
            if (!Friends.get().shouldAttack(p))
                return false;
            if (checkTeams.get() && isTeammate(p))
                return false;
        }
        return !(e instanceof AnimalEntity a) || !a.isBaby();
    }

    private boolean isTeammate(PlayerEntity p) {
        if (mc.player.isTeammate(p))
            return true;
        AbstractTeam myTeam = mc.player.getScoreboardTeam();
        AbstractTeam targetTeam = p.getScoreboardTeam();
        return myTeam != null && targetTeam != null && myTeam.getColor() == targetTeam.getColor();
    }

    private int getNearbyEnemiesCount(double range) {
        if (mc.world == null)
            return 0;
        Box box = mc.player.getBoundingBox().expand(range);
        return (int) mc.world.getOtherEntities(mc.player, box, this::isValid).stream().count();
    }

    private boolean canCrit() {
        return !mc.player.isOnGround() && mc.player.fallDistance > 0.0f
                && !mc.player.isClimbing() && !mc.player.isSubmergedInWater()
                && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                && mc.player.getVehicle() == null;
    }
}