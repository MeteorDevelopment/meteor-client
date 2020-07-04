package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.AttackEntityEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends ToggleModule {

    public Criticals() {
        super(Category.Combat, "criticals", "Critical attacks.");
    }

    @EventHandler
    private Listener<AttackEntityEvent> onAttackEntity = new Listener<>(event -> {
        if (!shouldDoCriticals()) return;

        double x = mc.player.x;
        double y = mc.player.y;
        double z = mc.player.z;

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y + 0.0625, z, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(x, y, z, false));
    });

    private boolean shouldDoCriticals() {
        boolean a = !mc.player.isSubmergedInWater() && !mc.player.isInLava() && !mc.player.isClimbing();
        if(!mc.player.onGround)return false;
        return a;
    }
}
