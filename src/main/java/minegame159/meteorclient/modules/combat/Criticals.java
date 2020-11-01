package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 18/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.atan2;

public class Criticals extends ToggleModule {
    public enum Mode {
        Packet,
        Jump,
        MiniJump
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Mode.")
            .defaultValue(Mode.Packet)
            .build()
    );

    public Criticals() {
        super(Category.Combat, "criticals", "Critical attacks.");
    }

    private boolean wasNoFallActive;

    private PlayerInteractEntityC2SPacket attackPacket;
    private HandSwingC2SPacket swingPacket;
    private boolean sendPackets;
    private int sendTimer;
    private boolean wasToggled = false;

    @Override
    public void onActivate() {
        wasNoFallActive = false;
        attackPacket = null;
        swingPacket = null;
        sendPackets = false;
        sendTimer = 0;
    }

    @EventHandler
    private final Listener<SendPacketEvent> onSendPacket = new Listener<>(event -> {

        if (event.packet instanceof PlayerInteractEntityC2SPacket && ((PlayerInteractEntityC2SPacket) event.packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.ATTACK) {
            if (!shouldDoCriticals()) return;
            if (mode.get() == Mode.Packet) doPacketMode();
            else doJumpMode(event);
        } else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Mode.Packet) {
            if (!shouldDoCriticals()) return;
            doJumpModeSwing(event);
        }
    });

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (sendPackets) {
            if (sendTimer <= 0) {
                sendPackets = false;

                if (attackPacket == null) return;
                mc.getNetworkHandler().sendPacket(attackPacket);
                mc.getNetworkHandler().sendPacket(swingPacket);

                attackPacket = null;
                swingPacket = null;

                onEnd();
            } else {
                sendTimer--;
            }
        }
    });

    private void doPacketMode() {
        onStart();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        Vec3d vec3d = new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        double yaw = Math.toRadians(getRotationFromVec3d(vec3d));

        if(sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z) > 0.2f) mc.player.setVelocity(sin(-yaw) * 0.2f, mc.player.getVelocity().y, cos(yaw) * 0.2f);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, false));

        onEnd();
    }

    private void doJumpMode(SendPacketEvent event) {
        if (!sendPackets) {
            onStart();

            sendPackets = true;
            sendTimer = mode.get() == Mode.Jump ? 6 : 4;
            attackPacket = (PlayerInteractEntityC2SPacket) event.packet;

            if (mode.get() == Mode.Jump) mc.player.jump();
            else ((IVec3d) mc.player.getVelocity()).setY(0.25);
            event.cancel();
        }
    }

    private void doJumpModeSwing(SendPacketEvent event) {
        if (sendPackets && swingPacket == null) {
            swingPacket = (HandSwingC2SPacket) event.packet;

            event.cancel();
        }
    }

    private void onStart() {
        wasNoFallActive = ModuleManager.INSTANCE.get(NoFall.class).isActive();

        if (wasNoFallActive) {
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }
    }

    private void onEnd() {
        if (wasNoFallActive) {
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }
    }

    private boolean shouldDoCriticals() {
        boolean a = !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isClimbing();
        if (!mc.player.isOnGround()) return false;
        return a;
    }

    private double getRotationFromVec3d(Vec3d vec3d){
        double x = vec3d.x;
        double y = vec3d.y;
        double z = vec3d.z;
        double speed = Math.sqrt(x * x + y * y + z * z);

        x /= speed;
        z /= speed;

        return Math.toDegrees(atan2(z, x))-90.0;
    }
}
