package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class Sprint extends Module {
    public static Sprint instance;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> unsprintOnHit = sgGeneral.add(new BoolSetting.Builder()
        .name("unsprint-on-hit")
        .description("攻击时自动停冲（W-Tap）")
        .defaultValue(true)
        .build());

    private final Setting<Integer> hitUnSprintTicks = sgGeneral.add(new IntSetting.Builder()
        .name("hit-unsprint-ticks")
        .description("攻击后停冲持续的 tick 数")
        .defaultValue(1).min(1).max(10).sliderMax(5).build());

    // 新增设置：方向检查开关
    private final Setting<Boolean> onlyForward = sgGeneral.add(new BoolSetting.Builder()
        .name("only-forward")
        .description("仅在按住前进键时触发 W-Tap（风筝敌人时不停冲）")
        .defaultValue(true)
        .build());

    public Sprint() {
        super(Categories.Movement, "sprint", "终极自动冲刺 • 智能W-Tap版");
        instance = this;
    }

    private final Random random = new Random();
    private int stopSprintTicks = 0;
    
    // 外部联动
    private boolean externalRequest = false;
    private Runnable externalCallback = null;
    public static int critReserve = 0;

    @Override
    public void onActivate() {
        instance = this;
        stopSprintTicks = 0;
        externalRequest = false;
        externalCallback = null;
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }

    public static void requestCritUnsprint(Runnable callback) {
        if (instance != null && instance.isActive()) {
            instance.externalRequest = true;
            instance.externalCallback = callback;
        } else {
            callback.run();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPreTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (externalRequest && externalCallback != null) {
            // TBot 请求的必暴，无论是否按W都执行（因为 TBot 判定过需要暴击）
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            externalCallback.run();
            externalRequest = false;
            externalCallback = null;
            stopSprintTicks = hitUnSprintTicks.get(); 
            return;
        }

        if (stopSprintTicks > 0) {
            stopSprintTicks--;
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return;
        }

        if (mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
             return; 
        }

        if (shouldSprint()) {
            if (!mc.player.isSprinting()) {
                mc.player.setSprinting(true);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }
    }

    private boolean shouldSprint() {
        return !mc.player.isSneaking() 
            && PlayerUtils.isMoving() 
            && mc.player.getHungerManager().getFoodLevel() > 6
            && !mc.player.horizontalCollision
            && mc.player.forwardSpeed > 0;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!unsprintOnHit.get() || stopSprintTicks > 0) return;
        
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override public void interact(Hand hand) {}
                @Override public void interactAt(Hand hand, Vec3d pos) {}
                @Override public void attack() {
                    // 水面检查
                    if (mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) return;
                    
                    // ==================== 智能方向检查优化 (已修复报错) ====================
                    if (onlyForward.get()) {
                        // 修复：改用 mc.options 检测按键，而不是 mc.player.input
                        if (mc.options == null || !mc.options.forwardKey.isPressed()) {
                            return;
                        }
                    }
                    
                    stopSprintTicks = hitUnSprintTicks.get();
                }
            });
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mc.player == null) return;
        boolean rPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rPressed && critReserve == 0) {
            critReserve = 1;
        } else if (!rPressed && critReserve > 0) {
            critReserve = 0;
        }
    }

    // Mixin 兼容
    public boolean rageSprint() { return false; }
    public boolean unsprintInWater() { return false; }
    public boolean stopSprinting() { return false; }
}