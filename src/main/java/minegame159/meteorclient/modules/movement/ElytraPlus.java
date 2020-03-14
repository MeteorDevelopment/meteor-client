package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

public class ElytraPlus extends Module {
    private Setting<Boolean> autoTakeOff = addSetting(new BoolSetting.Builder()
            .name("auto-take-off")
            .description("Automatically takes off when u hold jump without needing to double jump.")
            .defaultValue(true)
            .build()
    );

    private Setting<Double> fallMultiplier = addSetting(new DoubleSetting.Builder()
            .name("fall-multiplier")
            .description("Controls how fast will u go down naturally.")
            .defaultValue(0)
            .min(0)
            .build()
    );

    private Setting<Boolean> dontGoUpAndDownWhenMovingHorizontally = addSetting(new BoolSetting.Builder()
            .name("dont-go-up-and-down-when-moving-horizontally")
            .description("Doesn't go up or don't when moving horizontally.")
            .defaultValue(false)
            .build()
    );

    private Setting<Double> horizontalSpeed = addSetting(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("How fast will u go forward and backward.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private Setting<Double> verticalSpeed = addSetting(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("How fast will u go up and down.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private Setting<Boolean> stopInWater = addSetting(new BoolSetting.Builder()
            .name("stop-in-water")
            .description("Stops flying in water.")
            .defaultValue(true)
            .build()
    );

    private boolean lastJumpPressed;
    private boolean incrementJumpTimer;
    private int jumpTimer;

    private double velX, velY, velZ;
    private Vec3d forward, right;

    public ElytraPlus() {
        super(Category.Movement, "Elytra+", "Makes elytra better,");
    }

    @Override
    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        handleAutoTakeOff();

        if (mc.player.isFallFlying()) {
            velX = 0;
            velY = mc.player.getVelocity().y;
            velZ = 0;
            forward = Vec3d.fromPolar(0, mc.player.yaw).multiply(0.1);
            right = Vec3d.fromPolar(0, mc.player.yaw + 90).multiply(0.1);

            // Handle stopInWater
            if (mc.player.isTouchingWater() && stopInWater.get()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                return;
            }

            handleFallMultiplier();
            handleHorizontalSpeed();
            handleVerticalSpeed();

            mc.player.setVelocity(velX, velY, velZ);
        }
    });

    private void handleHorizontalSpeed() {
        if (mc.options.keyForward.isPressed()) {
            velX += forward.x * horizontalSpeed.get() * 10;
            velZ += forward.z * horizontalSpeed.get() * 10;
        } else if (mc.options.keyBack.isPressed()) {
            velX -= forward.x * horizontalSpeed.get() * 10;
            velZ -= forward.z * horizontalSpeed.get() * 10;
        }

        if (mc.options.keyRight.isPressed()) {
            velX += right.x * horizontalSpeed.get() * 10;
            velZ += right.z * horizontalSpeed.get() * 10;
        } else if (mc.options.keyLeft.isPressed()) {
            velX -= right.x * horizontalSpeed.get() * 10;
            velZ -= right.z * horizontalSpeed.get() * 10;
        }
    }

    private void handleVerticalSpeed() {
        if (mc.options.keyJump.isPressed()) {
            velY += 0.5 * verticalSpeed.get();
        } else if (mc.options.keySneak.isPressed()) {
            velY -= 0.5 * verticalSpeed.get();
        }
    }

    private void handleFallMultiplier() {
        if (velY < 0) velY *= fallMultiplier.get();
        else if (velY > 0) velY = 0;
    }

    private void handleAutoTakeOff() {
        if (incrementJumpTimer) jumpTimer++;

        boolean jumpPressed = mc.options.keyJump.isPressed();

        if (autoTakeOff.get() && jumpPressed) {
            if (!lastJumpPressed && jumpPressed&& !mc.player.isFallFlying()) {
                jumpTimer = 0;
                incrementJumpTimer = true;
            }

            if (jumpTimer >= 8) {
                jumpTimer = 0;
                incrementJumpTimer = false;
                mc.player.setJumping(false);
                mc.player.setSprinting(true);
                mc.player.jump();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        lastJumpPressed = jumpPressed;
    }
}
