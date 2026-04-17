package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;

public class Sprint extends Module {
    public static Sprint instance;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 只需要这个设置：暂停多久。Grim 推荐 4-6，太短会被 Simulation 检测。
    private final Setting<Integer> hitUnSprintTicks = sgGeneral.add(new IntSetting.Builder()
        .name("hit-unsprint-ticks").defaultValue(5).min(3).max(15).build());

    private final Setting<Boolean> stopOnHurt = sgGeneral.add(new BoolSetting.Builder()
        .name("stop-on-hurt").defaultValue(true).build());

    public Sprint() {
        super(Categories.Movement, "sprint", "GrimAC 纯净版");
        instance = this;
    }

    private int pauseTicks = 0;

    @Override
    public void onActivate() {
        instance = this;
        pauseTicks = 0;
    }

    @Override
    public void onDeactivate() {
        instance = null;
    }

    /**
     * 唯一的对外接口：暂停疾跑
     */
    public void pause() {
        pause(hitUnSprintTicks.get());
    }
    
    /**
     * 带参数的暂停方法，用于GrimAC兼容
     */
    public void pause(int ticks) {
        this.pauseTicks = ticks;
        if (mc.player != null) {
            mc.player.setSprinting(false);
            // 关键：同时解除按键绑定状态，防止原生逻辑干扰
            if (mc.options != null) mc.options.sprintKey.setPressed(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // 1. 处于暂停期，强行按死
        if (pauseTicks > 0) {
            pauseTicks--;
            mc.player.setSprinting(false);
            if (mc.options != null) mc.options.sprintKey.setPressed(false);
            return;
        }

        boolean strictSprint = !(mc.player.isPartlyTouchingWater())
            && !mc.player.hasBlindnessEffect()
            && mc.player.hasVehicle() ? (mc.player.getVehicle().canSprintAsVehicle() && mc.player.getVehicle().isLogicalSideForUpdatingMovement()) : mc.player.getHungerManager().canSprint()
            && (!mc.player.horizontalCollision || mc.player.collidedSoftly);

        // 3. 正常疾跑逻辑
        if (shouldSprint()) {
            mc.player.setSprinting(true);
            if (mc.options != null) mc.options.sprintKey.setPressed(true);
        } else {
            mc.player.setSprinting(false);
            if (mc.options != null && !mc.options.sprintKey.isDefault()) {
                mc.options.sprintKey.setPressed(false);
            }
        }
    }

    private boolean shouldSprint() {
        return !mc.player.isSneaking() 
            && mc.options.forwardKey.isPressed() 
            && !mc.player.horizontalCollision 
            && mc.player.getHungerManager().getFoodLevel() > 6
            && !mc.player.isTouchingWater();
    }
    
    // 删除了 onSendPacket 监听，防止逻辑双重触发

    public boolean rageSprint() { return false; }
    public boolean unsprintInWater() { return mc.player != null && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater(); }
    public boolean stopSprinting() { return pauseTicks > 0; }
    public void onAttackTriggered() { pause(); }
}