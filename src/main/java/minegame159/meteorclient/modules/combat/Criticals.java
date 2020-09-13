package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 18/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.AttackEntityEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.movement.NoFall;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.atan2;

public class Criticals extends ToggleModule {

    public Criticals() {
        super(Category.Combat, "criticals", "Critical attacks.");
    }

    private boolean wasActive = false;

    @EventHandler
    private final Listener<AttackEntityEvent> onAttackEntity = new Listener<>(event -> {
        if (!shouldDoCriticals()) return;
        if (ModuleManager.INSTANCE.get(NoFall.class).isActive()){
            wasActive = true;
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        Vec3d vec3d = new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
        double yaw = Math.toRadians(getRotationFromVec3d(vec3d));

        if(sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z) > 0.2f) mc.player.setVelocity(sin(-yaw) * 0.2f, mc.player.getVelocity().y, cos(yaw) * 0.2f);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, false));

        if (wasActive) {
            ModuleManager.INSTANCE.get(NoFall.class).toggle();
        }
    });

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
