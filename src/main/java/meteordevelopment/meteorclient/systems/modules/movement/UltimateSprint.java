package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.Random; // 【修改1：导入Random】

public class UltimateSprint extends Module {
    public static UltimateSprint instance;

    // 状态控制
    private boolean externalRequest = false;
    private Runnable externalCallback = null;
    private int currentDelayTicks = 0;

    // 【修改2：增加存随机数的变量】
    private int currentRandomTarget = 0;
    private final Random random = new Random();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ==================== 设置项 ====================

    // 【修改3：把原来的 attackDelay 拆分为 min 和 max】
    private final Setting<Integer> minDelay = sgGeneral.add(new IntSetting.Builder()
            .name("min-delay")
            .description("最小延迟 Tick (Grim建议2)")
            .defaultValue(2)
            .min(0).max(10).build());

    private final Setting<Integer> maxDelay = sgGeneral.add(new IntSetting.Builder()
            .name("max-delay")
            .description("最大延迟 Tick (Grim建议3)")
            .defaultValue(3)
            .min(0).max(10).build());

    private final Setting<Boolean> needRestoreSprint = sgGeneral.add(new BoolSetting.Builder()
            .name("need-restore-sprint")
            .description("攻击完成后是否自动恢复疾跑按键状态")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> needReCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("need-re-check")
            .description("攻击完成后是否需要重新检查目标是否存活")
            .defaultValue(true)
            .build());

    public UltimateSprint() {
        super(Categories.Movement, "ultimate-sprint", "Smart W-Tap: Handles attack callbacks with configurable delay.");
    }

    @Override
    public void onActivate() {
        instance = this;
        resetRequests();
    }

    @Override
    public void onDeactivate() {
        resetRequests();
        instance = null;
    }

    private void resetRequests() {
        externalRequest = false;
        externalCallback = null;
        currentDelayTicks = 0;
    }

    // 外部调用接口
    public static void requestCritUnsprint(Runnable callback) {
        if (instance != null && !instance.externalRequest) {
            instance.externalRequest = true;
            instance.externalCallback = callback;
            instance.currentDelayTicks = 0;

            // 【修改4：在这里生成随机数】
            int min = instance.minDelay.get();
            int max = instance.maxDelay.get();
            if (min > max) { int t = min; min = max; max = t; } // 防止填反
            
            // 生成 min 到 max 的随机数并存起来
            instance.currentRandomTarget = min + instance.random.nextInt(max - min + 1);
        }
    }

    public static void clearCritUnsprintRequest() {
        if (instance != null) {
            instance.resetRequests();
        }
    }

    public Entity getTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e == null || !e.isAlive()) {
                return null;
            } else {
                return e;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MEDIUM)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null)
            return;

        // --- 1. 基础移动同步 (WASD) ---
        if (mc.currentScreen == null) {
            mc.options.forwardKey.setPressed(Input.isKeyPressed(GLFW.GLFW_KEY_W));
            mc.options.backKey.setPressed(Input.isKeyPressed(GLFW.GLFW_KEY_S));
            mc.options.leftKey.setPressed(Input.isKeyPressed(GLFW.GLFW_KEY_A));
            mc.options.rightKey.setPressed(Input.isKeyPressed(GLFW.GLFW_KEY_D));
        } else {
            setMovingKeys(false);
            return;
        }

        // --- 2. 处理攻击请求与延迟逻辑 ---
        if (externalRequest && externalCallback != null) {
            // A. 强制停止疾跑
            mc.options.sprintKey.setPressed(false);
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }

            // B. 检查延迟
            // 【修改5：这里对比生成的随机数，而不是固定值】
            if (currentDelayTicks < currentRandomTarget) {
                currentDelayTicks++;
                return; // 延迟未到，直接退出
            } else {
                // 延迟已到，准备执行攻击
                boolean shouldAttack = true;

                if (needReCheck.get()) {
                    if (getTarget() == null) {
                        shouldAttack = false; 
                    }
                }

                if (shouldAttack) {
                    try {
                        externalCallback.run();
                    } catch (Exception e) {
                        e.printStackTrace(); 
                    }
                }

                resetRequests(); 
                
                return; 
            }
        }

        // --- 3. 常规疾跑逻辑 ---
        if (needRestoreSprint.get()) {
            boolean forward = mc.options.forwardKey.isPressed();
            boolean back = mc.options.backKey.isPressed();

            if (mc.player.getHungerManager() != null) {
                boolean canSprint = forward && !back
                        && !mc.player.isSneaking()
                        && !mc.player.horizontalCollision
                        && mc.player.getHungerManager().getFoodLevel() > 6;

                if (canSprint) {
                    mc.options.sprintKey.setPressed(true);
                } else {
                    mc.options.sprintKey.setPressed(false);
                }
            }
        }
    }

    private void setMovingKeys(boolean pressed) {
        mc.options.forwardKey.setPressed(pressed);
        mc.options.backKey.setPressed(pressed);
        mc.options.leftKey.setPressed(pressed);
        mc.options.rightKey.setPressed(pressed);
        mc.options.sprintKey.setPressed(pressed);
    }

    // 兼容性占位
    public boolean rageSprint() { return false; }
    public boolean unsprintInWater() { return false; }
    public boolean stopSprinting() { return !isActive(); }
}