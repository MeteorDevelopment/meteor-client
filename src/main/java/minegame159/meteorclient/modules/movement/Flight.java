package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.PreTickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;

public class Flight extends ToggleModule {
    public enum Mode {
        Abilities,
        Velocity
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Mode.")
            .defaultValue(Mode.Abilities)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Speed.")
            .defaultValue(0.1)
            .min(0.0)
            .build()
    );

    public Mode getMode() {
        return this.mode.get();
    }

    public Flight() {
        super(Category.Movement, "flight", "FLYYYY! You will take fall damage so enable no fall.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        }
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.abilities.flying = false;
            mc.player.abilities.setFlySpeed(0.05f);
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = false;
        }
    }

    private boolean flip;
    private float lastYaw;

    @EventHandler
    private final Listener<PreTickEvent> onPreTick = new Listener<>(event -> {
        float currentYaw = mc.player.yaw;
        if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getVelocity().length() < 0.003d) {
            mc.player.yaw += flip ? 1 : -1;
            flip = !flip;
        }
        lastYaw = currentYaw;
    });

    @EventHandler
    private final Listener<PostTickEvent> onPostTick = new Listener<>(event -> {
        if (mc.player.yaw != lastYaw) {
            mc.player.yaw = lastYaw;
        }

        if (mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
            mc.player.abilities.setFlySpeed(speed.get().floatValue());
            mc.player.abilities.flying = true;
            if (mc.player.abilities.creativeMode) return;
            mc.player.abilities.allowFlying = true;
        } else if (mode.get() == Mode.Velocity) {
            // TODO: deal with underwater movement, find a way to "spoof" not being in water
            // also, all of the multiplication below is to get the speed to roughly match the speed
            // you get when using vanilla fly
            mc.player.abilities.flying = false;
            mc.player.flyingSpeed = speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);

            mc.player.setVelocity(0, 0, 0);
            Vec3d initialVelocity = mc.player.getVelocity();

            if (mc.options.keyJump.isPressed()) {
                mc.player.setVelocity(initialVelocity.add(0, speed.get() * 5f, 0));
            }
            if (mc.options.keySneak.isPressed()) {
                mc.player.setVelocity(initialVelocity.subtract(0, speed.get() * 5f, 0));
            }
        }
    });

    private long lastModifiedTime = 0;
    private double lastY = Double.MAX_VALUE;

    /**
     * @see ServerPlayNetworkHandler#onPlayerMove(PlayerMoveC2SPacket)
     */
    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {
        if (!(event.packet instanceof PlayerMoveC2SPacket)) {
            return;
        }

        PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
        long currentTime = System.currentTimeMillis();
        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            // maximum time we can be "floating" is 80 ticks, so 4 seconds max
            if (currentTime - lastModifiedTime > 1000
                    && lastY != Double.MAX_VALUE
                    && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir()) {
                // actual check is for >= -0.03125D but we have to do a bit more than that
                // probably due to compression or some shit idk
                ((IPlayerMoveC2SPacket) packet).setY(lastY - 0.03130D);
                lastModifiedTime = currentTime;
            } else {
                lastY = currentY;
            }
        }
    });
}
