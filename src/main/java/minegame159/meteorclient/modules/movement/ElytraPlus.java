package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PlayerMoveEvent;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

public class ElytraPlus extends ToggleModule {
    private Setting<Boolean> autoTakeOff = addSetting(new BoolSetting.Builder()
            .name("auto-take-off")
            .description("Automatically takes off when u hold jump without needing to double jump.")
            .defaultValue(false)
            .build()
    );

    private Setting<Double> fallMultiplier = addSetting(new DoubleSetting.Builder()
            .name("fall-multiplier")
            .description("Controls how fast will u go down naturally.")
            .defaultValue(0.01)
            .min(0)
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

    private Setting<Boolean> dontGoIntoUnloadedChunks = addSetting(new BoolSetting.Builder()
            .name("dont-go-into-unloaded-chunks")
            .description("Dont go into unloaded chunks.")
            .defaultValue(true)
            .build()
    );

    private Setting<Double> autopilotMinimumHeight;
    private Setting<Boolean> autopilot = addSetting(new BoolSetting.Builder()
            .name("autopilot")
            .description("Automatically flies forward maintaining minimum height.")
            .group("Autopilot")
            .defaultValue(false)
            .onChanged(aBoolean -> {
                if (isActive() && !aBoolean) ((IKeyBinding) mc.options.keyForward).setPressed(false);
                autopilotMinimumHeight.setVisible(aBoolean);
            })
            .build()
    );

    private boolean lastJumpPressed;
    private boolean incrementJumpTimer;
    private int jumpTimer;

    private double velX, velY, velZ;
    private Vec3d forward, right;

    private boolean decrementFireworkTimer;
    private int fireworkTimer;

    private boolean lastForwardPressed;

    public ElytraPlus() {
        super(Category.Movement, "Elytra+", "Makes elytra better,");

        autopilotMinimumHeight = addSetting(new DoubleSetting.Builder()
                .name("minimum-height")
                .description("Autopilot minimum height.")
                .group("Autopilot")
                .defaultValue(160)
                .min(0)
                .sliderMax(260)
                .visible(false)
                .build()
        );
    }

    @Override
    public void onActivate() {
        lastJumpPressed = false;
        jumpTimer = 0;
    }

    @Override
    public void onDeactivate() {
        if (autopilot.get()) ((IKeyBinding) mc.options.keyForward).setPressed(false);
    }

    @EventHandler
    private Listener<PlayerMoveEvent> onPlayerMove = new Listener<>(event -> {
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        handleAutoTakeOff();

        if (mc.player.isFallFlying()) {
            velX = 0;
            velY = event.movement.y;
            velZ = 0;
            forward = Vec3d.fromPolar(0, mc.player.yaw).multiply(0.1);
            right = Vec3d.fromPolar(0, mc.player.yaw + 90).multiply(0.1);

            // Handle stopInWater
            if (mc.player.isTouchingWater() && stopInWater.get()) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                return;
            }

            handleFallMultiplier();
            handleAutopilot();

            handleHorizontalSpeed();
            handleVerticalSpeed();

            int chunkX = (int) ((mc.player.getX() + velX) / 16);
            int chunkZ = (int) ((mc.player.getZ() + velZ) / 16);
            if (dontGoIntoUnloadedChunks.get()) {
                if (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    ((IVec3d) event.movement).set(velX, velY, velZ);
                } else {
                    ((IVec3d) event.movement).set(0, velY, 0);
                }
            } else ((IVec3d) event.movement).set(velX, velY, velZ);
        } else {
            if (lastForwardPressed) {
                ((IKeyBinding) mc.options.keyForward).setPressed(false);
                lastForwardPressed = false;
            }
        }
    });

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        if (decrementFireworkTimer) {
            if (fireworkTimer <= 0) decrementFireworkTimer = false;

            fireworkTimer--;
        }
    });

    private void handleAutopilot() {
        if (autopilot.get()) {
            ((IKeyBinding) mc.options.keyForward).setPressed(true);

            if (mc.player.getY() < autopilotMinimumHeight.get() && !decrementFireworkTimer) {
                int slot = InvUtils.findItemInHotbar(Items.FIREWORK_ROCKET, itemStack -> true);
                if (slot != -1) {
                    mc.player.inventory.selectedSlot = slot;
                    Utils.rightClick();

                    decrementFireworkTimer = true;
                    fireworkTimer = 20;
                } else {
                    Utils.sendMessage("#blueElytra+ Autopilot:#white Disabled autopilot because you don't have any fireworks left in your hotbar.");
                    autopilot.set(false);
                }
            }

            if (fireworkTimer > 0) {
                velY = 2;
            }

            lastForwardPressed = true;
        }
    }

    private void handleHorizontalSpeed() {
        boolean a = false;
        boolean b = false;

        if (mc.options.keyForward.isPressed()) {
            velX += forward.x * horizontalSpeed.get() * 10;
            velZ += forward.z * horizontalSpeed.get() * 10;
            a = true;
        } else if (mc.options.keyBack.isPressed()) {
            velX -= forward.x * horizontalSpeed.get() * 10;
            velZ -= forward.z * horizontalSpeed.get() * 10;
            a = true;
        }

        if (mc.options.keyRight.isPressed()) {
            velX += right.x * horizontalSpeed.get() * 10;
            velZ += right.z * horizontalSpeed.get() * 10;
            b = true;
        } else if (mc.options.keyLeft.isPressed()) {
            velX -= right.x * horizontalSpeed.get() * 10;
            velZ -= right.z * horizontalSpeed.get() * 10;
            b = true;
        }

        if (a && b) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
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
